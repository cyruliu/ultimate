package de.uni_freiburg.informatik.ultimate.lib.srparse.pattern;

import de.uni_freiburg.informatik.ultimate.lib.pea.CDD;

public class ResponseChain12Pattern extends PatternType {
	@Override
	public void transform() {
		final CDD p_cdd = mCdds.get(2);
		final CDD q_cdd = mScope.getCdd1();
		final CDD r_cdd = mScope.getCdd2();
		final CDD s_cdd = mCdds.get(1);
		final CDD t_cdd = mCdds.get(0);

		mPea = mPeaTransformator.responseChainPattern12(mId, p_cdd, q_cdd, r_cdd, s_cdd, t_cdd, mScope.toString());
	}

	@Override
	public String toString() {
		String res = new String();

		res = "it is always the case that if \"" + mCdds.get(2) + "\" holds, then \"" + mCdds.get(1)
				+ "\" eventually holds and is succeeded by \"" + mCdds.get(0) + "\"";

		return res;
	}
}
