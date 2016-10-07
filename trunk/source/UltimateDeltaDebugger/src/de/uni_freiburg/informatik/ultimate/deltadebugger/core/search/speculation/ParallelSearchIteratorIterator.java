package de.uni_freiburg.informatik.ultimate.deltadebugger.core.search.speculation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import de.uni_freiburg.informatik.ultimate.deltadebugger.core.exceptions.MissingTestResultException;
import de.uni_freiburg.informatik.ultimate.deltadebugger.core.exceptions.UncheckedInterruptedException;
import de.uni_freiburg.informatik.ultimate.deltadebugger.core.search.SearchStep;

/**
 * Runs a speculative search on an arbitrary number of worker threads with a
 * given test function.
 *
 * @param <T>
 *            search step type
 */
public class ParallelSearchIteratorIterator<T extends SearchStep<?, T>> {
	private final SpeculativeSearchIterator<T> searchIterator;
	private final CancelableStepTest<T> cancelableTest;
	private final List<Future<?>> pendingWorkers = new ArrayList<>();
	private volatile boolean stopRequested;

	/**
	 * Constructs a new instance that can be used for one iteration.
	 * 
	 * Note that all concurrent access to the SpeculativeSearchIterator is
	 * synchronized on the searchIterator instance and only the test function if
	 * executed without any synchronization.
	 * 
	 * @param searchIterator
	 *            speculative iterator to work on
	 * @param cancelableTest
	 *            test function that is called to determine test results
	 */
	public ParallelSearchIteratorIterator(SpeculativeSearchIterator<T> searchIterator,
			CancelableStepTest<T> cancelableTest) {
		this.searchIterator = searchIterator;
		this.cancelableTest = cancelableTest;
	}

	public boolean isStopRequested() {
		return stopRequested;
	}

	/**
	 * Request workers to stop the iteration.
	 */
	public void stopWorkers() {
		stopRequested = true;
	}

	/**
	 * @return the current step of the non-speculative iteration
	 */
	public T getCurrentStep() {
		synchronized (searchIterator) {
			return searchIterator.getCurrentStep();
		}
	}

	/**
	 * Begin and wait for iteration to end, then return the step.
	 * 
	 * @param executorService
	 * @param workerCount
	 * @return the step reached  at the end of iteration
	 */
	public T iterateToEnd(ExecutorService executorService, int workerCount) {
		beginIteration(executorService, workerCount);
		try {
			return endIteration();
		} catch (InterruptedException unexpected) {
			Thread.currentThread().interrupt();
			throw new UncheckedInterruptedException(unexpected);
		}
	}

	/**
	 * Start iteration using the given number of worker threads that are started
	 * by the given executor service.
	 * 
	 * @param executorService
	 * @param workerCount
	 */
	public void beginIteration(ExecutorService executorService, int workerCount) {
		if (workerCount < 1) {
			throw new IllegalArgumentException();
		}
		if (!pendingWorkers.isEmpty()) {
			throw new IllegalStateException("beginIteration already called");
		}
		for (int i = 0; i != workerCount; ++i) {
			pendingWorkers.add(executorService.submit(this::worker));
		}
	}

	/**
	 * Checks if iteration has ended (either successfully or non-successfully)
	 * without blocking.
	 * 
	 * @return true if iteration has ended
	 */
	public boolean pollIsDone() {
		if (pendingWorkers.isEmpty()) {
			throw new IllegalStateException("beginIteration has not been called");
		}
		return pendingWorkers.stream().allMatch(Future::isDone);
	}

	/**
	 * Wait until iteration has ended for a limited timespan only.
	 * 
	 * @param timeout
	 * @param unit
	 * @return true if all workers have ended at the time of return
	 * @throws InterruptedException
	 */
	public boolean waitForEnd(long timeout, TimeUnit unit) throws InterruptedException {
		long nanosLeft = unit.toNanos(timeout);
		final long deadline = System.nanoTime() + nanosLeft;

		for (Future<?> f : pendingWorkers) {
			if (!f.isDone()) {
				if (nanosLeft <= 0L) {
					return false;
				}
				try {
					f.get(nanosLeft, TimeUnit.NANOSECONDS);
				} catch (CancellationException | ExecutionException e) {
					// deferr exception to endIteration
				} catch (TimeoutException e) {
					return false;
				}
				nanosLeft = deadline - System.nanoTime();
			}
		}
		return true;
	}

	/**
	 * Wait until iteration has ended and return the result.
	 * 
	 * @return current result.
	 * @throws InterruptedException
	 */
	public T endIteration() throws InterruptedException {
		if (pendingWorkers.isEmpty()) {
			throw new IllegalStateException("beginIteration has not been called");
		}

		// Wait for all workers to return.
		// This is important to ensure that
		// - no exceptions are swallowed
		// - no new (potentially expensive) tests are started before the
		// previous ones have completed
		// - all parallel execution is limited to this method
		try {
			for (Future<?> f : pendingWorkers) {
				f.get();
			}
		} catch (ExecutionException e) {
			Throwable inner = e.getCause();
			if (inner instanceof Error) {
				throw (Error) inner;
			}
			if (inner instanceof RuntimeException) {
				throw (RuntimeException) inner;
			}
			throw new RuntimeException("unexpected sneaky exception", e);
		}

		return getCurrentStep();
	}

	private void worker() {
		try {
			while (!isStopRequested()) {
				final SpeculativeTask<T> task = getNextTask();
				if (task.isDone()) {
					return;
				}
				runTestAndCompleteTask(task);
			}
		} catch (Exception e) {
			stopWorkers();
			throw e;
		}
	}

	private SpeculativeTask<T> getNextTask() {
		while (true) {
			synchronized (searchIterator) {
				final SpeculativeTask<T> task = searchIterator.getNextTask();
				if (task != null) {
					return task;
				}
			}

			// Currently there is no speculative step available,
			// but not all pending tasks have completed yet so there
			// may be further steps available later
			// -> wait for more tasks to complete
			// Should use a more sophisiticated event mechanism to wake
			// up once another task has completed, and also ensure
			// that there is another task pending...
			try {
				TimeUnit.MILLISECONDS.sleep(10);
			} catch (InterruptedException e) {
				// There is no expected interruption, if this happens it's
				// like any other unexpected runtime exception
				Thread.currentThread().interrupt();
				throw new UncheckedInterruptedException(e);
			}
		}
	}

	private void runTestAndCompleteTask(SpeculativeTask<T> task) {
		final BooleanSupplier isCanceled = () -> task.isCanceled() || isStopRequested();
		final Optional<Boolean> result = cancelableTest.test(task.getStep(), isCanceled);
		if (!result.isPresent()) {
			// A test is only allowed to return no result if cancelation
			// was actually requested. To handle this case we could only
			// abort the whole iteration or repeat the test.
			// Iteration control is not the responsibility of the test
			// function and repeating the test with a broken
			// test function sounds like bad idea.
			if (!isCanceled.getAsBoolean()) {
				throw new MissingTestResultException();
			}
			return;
		}
		synchronized (searchIterator) {
			task.complete(result.get());
		}
	}
}