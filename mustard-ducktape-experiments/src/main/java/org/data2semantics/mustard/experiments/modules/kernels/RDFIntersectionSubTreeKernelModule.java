package org.data2semantics.mustard.experiments.modules.kernels;

import org.data2semantics.mustard.kernels.data.RDFData;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFIntersectionSubTreeKernel;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;

@Module(name="RDFIntersectionSubTreeKernel")
public class RDFIntersectionSubTreeKernelModule extends AbstractKernelModule<RDFData> {
	
	public RDFIntersectionSubTreeKernelModule(
			@In(name="depth") Integer depth,
			@In(name="discountFactor") Double discountFactor,
			@In(name="inference") Boolean inference, 
			@In(name="normalize") Boolean normalize,
			@In(name="graphData") RDFData graphData) {
		
		super(new RDFIntersectionSubTreeKernel(depth, discountFactor, inference, normalize), graphData);
	}
	
	@Override
	@Main
	public double[][] compute() {
		return super.compute();
	}
	
	@Override
	@Out(name="matrix")
	public double[][] getMatrix() {
		return super.getMatrix();
	}

	@Override
	@Out(name="runtime")
	public Long getRuntime() {
		return super.getRuntime();
	}
}
