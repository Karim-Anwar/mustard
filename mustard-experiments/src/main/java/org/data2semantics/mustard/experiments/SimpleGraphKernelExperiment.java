package org.data2semantics.mustard.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.data2semantics.mustard.experiments.utils.Result;
import org.data2semantics.mustard.kernels.Kernel;
import org.data2semantics.mustard.kernels.KernelUtils;
import org.data2semantics.mustard.kernels.data.GraphData;
import org.data2semantics.mustard.kernels.graphkernels.GraphKernel;
import org.data2semantics.mustard.learners.Prediction;
import org.data2semantics.mustard.learners.evaluation.EvaluationFunction;
import org.data2semantics.mustard.learners.libsvm.LibSVM;
import org.data2semantics.mustard.learners.libsvm.LibSVMParameters;


public class SimpleGraphKernelExperiment<D extends GraphData> extends KernelExperiment<GraphKernel<D>> {
	private D data;
	private List<Double> labels;
	private LibSVMParameters svmParms;
	private Result compR;

	public SimpleGraphKernelExperiment(List<? extends GraphKernel<D>> kernels, D data,
			List<Double> labels, LibSVMParameters svmParms, long[] seeds,
			List<EvaluationFunction> evalFunctions) {
		super(kernels, seeds);
		this.data = data;
		this.labels = labels;
		this.svmParms = svmParms;

		for (EvaluationFunction evalFunc : evalFunctions) {
			Result res = new Result(evalFunc);
			double[] resA = new double[seeds.length]; // add a new empty array with the length of the amount of seeds (i.e. the number of repetitions of the experiment).
			res.setScores(resA);
			results.add(res);
		}

		compR = new Result();
		results.add(compR);
	}

	@Override
	public void run() {
		long tic, toc;

		List<Double> tempLabels = new ArrayList<Double>(); // Copy labels, since it is supplied to multiple experiments potentially
		tempLabels.addAll(labels);

		Map<Kernel, double[][]> matrices = new HashMap<Kernel, double[][]>();

		tic = System.currentTimeMillis();	
		System.out.print("Computing kernels");	
		for (GraphKernel<D> kernel : kernels) {
			double[][] matrix = kernel.compute(data);
			matrices.put(kernel, matrix);
			System.out.print(".");
		}
		System.out.print("\n");
		toc = System.currentTimeMillis();

		compR.setLabel("kernel comp time");

		System.out.println("Performing CV...");
		for (int j = 0; j < seeds.length; j++) {
			for (Kernel k : matrices.keySet()) {
				matrices.put(k, KernelUtils.shuffle(matrices.get(k), seeds[j]));
			}
			Collections.shuffle(tempLabels, new Random(seeds[j]));		

			double[] target = new double[tempLabels.size()];
			for (int i = 0; i < target.length; i++) {
				target[i] = tempLabels.get(i);
			}

			Prediction[] pred = LibSVM.crossValidateWithMultipleKernels(matrices, target, svmParms, svmParms.getNumFolds());

			for (Result res : results) {
				if (res.getEval() != null) {
					res.getScores()[j] = res.getEval().computeScore(target, pred);	
				}
			}
			
			for (Prediction p : pred) {
				if (!usedKernels.containsKey(p.getUsedKernel())) {
					usedKernels.put(p.getUsedKernel(), 0.0);
				}
				usedKernels.put(p.getUsedKernel(), usedKernels.get(p.getUsedKernel()) + 1.0);
			}
		}
		
		for (Kernel k : usedKernels.keySet()) {
			usedKernels.put(k, usedKernels.get(k) / (seeds.length * labels.size()));
		}

		double[] comp = {toc - tic};
		compR.setScores(comp);		
	}
}