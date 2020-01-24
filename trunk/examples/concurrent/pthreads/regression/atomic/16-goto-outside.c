//#Safe
/*
 * Test support for GOTO: Leaving the atomic block.
 *
 * Author: Dominik Klumpp (klumpp@informatik.uni-freiburg.de)
 * Date: 2020-01-24
 *
 */

#include <pthread.h>
extern void __VERIFIER_atomic_begin();
extern void __VERIFIER_atomic_end();

typedef unsigned long int pthread_t;
int x = 0;
int y;

void *foo(void *arg) {
  //@ assert x == 0;
  return (void*)NULL;
}

int main() {
  pthread_t threadId;
  pthread_create(&threadId, NULL, & foo, NULL);

  __VERIFIER_atomic_begin();
  x = x + 1;
  if (x != y) {
    goto LABEL;
  }
  x = x - y;
  __VERIFIER_atomic_end();

  LABEL:
  x = 0;
}
