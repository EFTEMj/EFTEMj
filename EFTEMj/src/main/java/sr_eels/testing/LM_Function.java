package sr_eels.testing;

/**
 * Caller implement this interface to specify the function to be minimised and its gradient.
 */
public interface LM_Function {

    /**
     * x is a single point, but domain may be mulidimensional
     */
    double val(double[] x, double[] a);

    /**
     * return the kth component of the gradient df(x,a)/da_k
     */
    double grad(double[] x, double[] a, int ak);

} // LMfunc

