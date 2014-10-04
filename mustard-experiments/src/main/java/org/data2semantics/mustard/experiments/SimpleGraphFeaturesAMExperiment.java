package org.data2semantics.mustard.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.data2semantics.mustard.experiments.rescal.RESCALKernel;
import org.data2semantics.mustard.experiments.utils.AMDataSet;
import org.data2semantics.mustard.experiments.utils.BGSDataSet;
import org.data2semantics.mustard.experiments.utils.ClassificationDataSet;
import org.data2semantics.mustard.experiments.utils.LargeClassificationDataSet;
import org.data2semantics.mustard.experiments.utils.Result;
import org.data2semantics.mustard.experiments.utils.ResultsTable;
import org.data2semantics.mustard.experiments.utils.SimpleGraphFeatureVectorKernelExperiment;
import org.data2semantics.mustard.kernels.data.GraphList;
import org.data2semantics.mustard.kernels.data.RDFData;
import org.data2semantics.mustard.kernels.data.SingleDTGraph;
import org.data2semantics.mustard.kernels.graphkernels.CombinedKernel;
import org.data2semantics.mustard.kernels.graphkernels.FeatureVectorKernel;
import org.data2semantics.mustard.kernels.graphkernels.GraphKernel;
import org.data2semantics.mustard.kernels.graphkernels.graphlist.PathCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.graphlist.PathCountKernelMkII;
import org.data2semantics.mustard.kernels.graphkernels.graphlist.TreePathCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.graphlist.WLSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFPathCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFIntersectionTreeEdgeVertexPathKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFRootPathCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreePathCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWLSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWLRootSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWLSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.singledtgraph.RDFDTGraphPathCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.singledtgraph.RDFDTGraphRootPathCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.singledtgraph.RDFDTGraphTreePathCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.singledtgraph.RDFDTGraphTreeWLSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.singledtgraph.RDFDTGraphWLRootSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.singledtgraph.RDFDTGraphWLSubTreeKernel;
import org.data2semantics.mustard.learners.evaluation.Accuracy;
import org.data2semantics.mustard.learners.evaluation.EvaluationFunction;
import org.data2semantics.mustard.learners.evaluation.EvaluationUtils;
import org.data2semantics.mustard.learners.evaluation.F1;
import org.data2semantics.mustard.learners.liblinear.LibLINEARParameters;
import org.data2semantics.mustard.learners.libsvm.LibSVMParameters;
import org.data2semantics.mustard.rdf.DataSetUtils;
import org.data2semantics.mustard.rdf.RDFDataSet;
import org.data2semantics.mustard.rdf.RDFFileDataSet;
import org.data2semantics.mustard.rdf.RDFSingleDataSet;
import org.data2semantics.mustard.rdf.RDFUtils;
import org.data2semantics.mustard.util.Pair;
import org.data2semantics.mustard.weisfeilerlehman.StringLabel;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;

public class SimpleGraphFeaturesAMExperiment {
	private static final String AM_FOLDER =  "C:\\Users\\Gerben\\Dropbox\\AM_data";
	private static final String BGS_FOLDER = "C:\\Users\\Gerben\\Dropbox\\data_bgs_ac_uk_ALL";

	private static List<Resource> instances;
	private static List<Value> labels;
	private static List<Statement> blackList;
	private static List<Double> target;
	private static RDFDataSet tripleStore;

	/**
	 * @param args
	 */
	public static void main(String[] args) {


		//tripleStore = new RDFFileDataSet(AM_FOLDER, RDFFormat.TURTLE);
		//LargeClassificationDataSet ds = new AMDataSet(tripleStore, 10, 0.01, 5, 4);

		tripleStore = new RDFFileDataSet(BGS_FOLDER, RDFFormat.NTRIPLES);
		LargeClassificationDataSet ds = new BGSDataSet(tripleStore, "http://data.bgs.ac.uk/ref/Lexicon/hasTheme", 10, 0.05, 5, 3);

		List<EvaluationFunction> evalFuncs = new ArrayList<EvaluationFunction>();
		evalFuncs.add(new Accuracy());
		evalFuncs.add(new F1());

		ResultsTable resTable = new ResultsTable();
		resTable.setDigits(3);
		resTable.setManWU(0.05);

		long[] seeds = {11};
		long[] seedsDataset = {11,21,31,41,51}; //,61,71,81,91,101};
		double[] cs = {1};	

		LibLINEARParameters svmParms = new LibLINEARParameters(LibLINEARParameters.SVC_DUAL, cs);
		//svmParms.setDoCrossValidation(false);
		svmParms.setNumFolds(5);

		//svmParms.setEvalFunction(new F1());

		/*
		svmParms.setWeightLabels(EvaluationUtils.computeWeightLabels(target));
		svmParms.setWeights(EvaluationUtils.computeWeights(target));
		//*/

		double fraction = 0.05;
		int minClassSize = 0;
		int maxNumClasses = 3;


		boolean reverseWL = true; // WL should be in reverse mode, which means regular subtrees
		boolean[] inference = {false,true};

		int[] depths = {1,2,3};
		int[] pathDepths = {2,4,6};
		int[] iterationsWL = {2,4,6};

		boolean depthTimesTwo = true;



		Map<Long, Map<Boolean, Map<Integer,Pair<SingleDTGraph, List<Double>>>>> cache = createDataSetCache(ds, seedsDataset, fraction, minClassSize, maxNumClasses, depths, inference);
		tripleStore = null;


		///* The baseline experiment, BoW (or BoL if you prefer)
		for (boolean inf : inference) {
			resTable.newRow("Baseline BoL: " + inf);
			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();


					List<RDFDTGraphWLSubTreeKernel> kernelsBaseline = new ArrayList<RDFDTGraphWLSubTreeKernel>();	
					kernelsBaseline.add(new RDFDTGraphWLSubTreeKernel(0, d, reverseWL, false, true));

					//Collections.shuffle(target);
					SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph> exp = new SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph>(kernelsBaseline, data, target, svmParms, seeds, evalFuncs);
					exp.run();

					if (tempRes.isEmpty()) {
						for (Result res : exp.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
			}
		}

		System.out.println(resTable);

		//*/

		/*
		for (boolean inf : inference) {
			resTable.newRow("Path Count through root: " + inf);	
			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();

					List<RDFDTGraphRootPathCountKernel> kernels = new ArrayList<RDFDTGraphRootPathCountKernel>();	

					if (depthTimesTwo) {
						kernels.add(new RDFDTGraphRootPathCountKernel(d*2, true, true));
					} else {
						for (int dd : pathDepths) {
							kernels.add(new RDFDTGraphRootPathCountKernel(dd, true, true));
						}
					}

					SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph> exp = new SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph>(kernels, data, target, svmParms, seeds, evalFuncs);
					exp.run();

					if (tempRes.isEmpty()) {
						for (Result res : exp.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
			}
		}

		System.out.println(resTable);

		//*/

		/*
		for (boolean inf : inference) {
			resTable.newRow("WL through root: " + inf);
			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();

					List<RDFDTGraphWLRootSubTreeKernel> kernels = new ArrayList<RDFDTGraphWLRootSubTreeKernel>();	

					if (depthTimesTwo) {
						kernels.add(new RDFDTGraphWLRootSubTreeKernel(d*2, d, reverseWL, false, true));
					} else {
						for (int dd : pathDepths) {
							kernels.add(new RDFDTGraphWLRootSubTreeKernel(dd, d, reverseWL, false, true));
						}
					}

					SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph> exp = new SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph>(kernels, data, target, svmParms, seeds, evalFuncs);
					exp.run();

					if (tempRes.isEmpty()) {
						for (Result res : exp.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
			}
		}

		System.out.println(resTable);

		//*/

		/*
		for (boolean inf : inference) {
			resTable.newRow("Path Count Tree: " + inf);	

			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();

					List<RDFDTGraphTreePathCountKernel> kernels = new ArrayList<RDFDTGraphTreePathCountKernel>();		

					if (depthTimesTwo) {
						kernels.add(new RDFDTGraphTreePathCountKernel(d*2, true));
					} else {
						for (int dd : pathDepths) {
							kernels.add(new RDFDTGraphTreePathCountKernel(dd, true));
						}
					}

					SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph> exp = new SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph>(kernels, data, target, svmParms, seeds, evalFuncs);
					exp.run();

					if (tempRes.isEmpty()) {
						for (Result res : exp.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
			}
		}

		System.out.println(resTable);

		//*/

		/*
		for (boolean inf : inference) {
			resTable.newRow("WL Tree: " + inf);	

			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();

					List<RDFDTGraphTreeWLSubTreeKernel> kernels = new ArrayList<RDFDTGraphTreeWLSubTreeKernel>();	

					if (depthTimesTwo) {
						kernels.add(new RDFDTGraphTreeWLSubTreeKernel(d*2, d, reverseWL, false, true));
					} else {
						for (int dd : pathDepths) {
							kernels.add(new RDFDTGraphTreeWLSubTreeKernel(dd, d, reverseWL, false, true));
						}
					}

					SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph> exp = new SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph>(kernels, data, target, svmParms, seeds, evalFuncs);
					exp.run();

					if (tempRes.isEmpty()) {
						for (Result res : exp.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
			}
		}

		System.out.println(resTable);

		//*/


		/* RDF Path Count 
		for (boolean inf : inference) {
			resTable.newRow("RDF Path Count: " + inf);

			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();

					List<RDFDTGraphPathCountKernel> kernels = new ArrayList<RDFDTGraphPathCountKernel>();	

					if (depthTimesTwo) {
						kernels.add(new RDFDTGraphPathCountKernel(d*2, d, true));
					} else {
						for (int dd : pathDepths) {
							kernels.add(new RDFDTGraphPathCountKernel(dd, d, true));
						}
					}

					SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph> exp = new SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph>(kernels, data, target, svmParms, seeds, evalFuncs);
					exp.run();

					if (tempRes.isEmpty()) {
						for (Result res : exp.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
			}
		}

		System.out.println(resTable);

		//*/


		/* RDF WL
		for (boolean inf : inference) {
			resTable.newRow("RDF WL: " + inf);

			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();

					List<RDFDTGraphWLSubTreeKernel> kernels = new ArrayList<RDFDTGraphWLSubTreeKernel>();	

					if (depthTimesTwo) {
						kernels.add(new RDFDTGraphWLSubTreeKernel(d*2, d, reverseWL, false, true));
					} else {
						for (int dd : iterationsWL) {
							kernels.add(new RDFDTGraphWLSubTreeKernel(dd, d, reverseWL, false, true));
						}
					}

					SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph> exp = new SimpleGraphFeatureVectorKernelExperiment<SingleDTGraph>(kernels, data, target, svmParms, seeds, evalFuncs);
					exp.run();

					if (tempRes.isEmpty()) {
						for (Result res : exp.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
			}
		}

		System.out.println(resTable);

		//*/




		///* Regular WL
		for (boolean inf : inference) {
			resTable.newRow("Regular WL: " + inf);		
			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();

					List<DTGraph<String,String>> graphs = RDFUtils.getSubGraphs(data.getGraph(), data.getInstances(), d);

					double avgNodes = 0;
					double avgLinks = 0;

					for (DTGraph<String,String> g : graphs){
						avgNodes += g.nodes().size();
						avgLinks += g.links().size();
					}
					avgNodes /= graphs.size();
					avgLinks /= graphs.size();

					System.out.println("Avg # nodes: " + avgNodes + " , avg # links: " + avgLinks);

					List<WLSubTreeKernel> kernels = new ArrayList<WLSubTreeKernel>();

					for (int dd : iterationsWL) {
						WLSubTreeKernel kernel = new WLSubTreeKernel(dd, reverseWL, true);			
						kernels.add(kernel);
					}

					//resTable.newRow(kernels.get(0).getLabel() + "_" + inf);
					SimpleGraphFeatureVectorKernelExperiment<GraphList<DTGraph<String,String>>> exp2 = new SimpleGraphFeatureVectorKernelExperiment<GraphList<DTGraph<String,String>>>(kernels, new GraphList<DTGraph<String,String>>(graphs), target, svmParms, seeds, evalFuncs);

					//System.out.println(kernels.get(0).getLabel());
					exp2.run();
					
					if (tempRes.isEmpty()) {
						for (Result res : exp2.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp2.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
				System.out.println(resTable);
			}
		}
		//*/


		resTable.addCompResults(resTable.getBestResults());
		System.out.println(resTable);

		
		///* Path Count full
		for (boolean inf : inference) {
			resTable.newRow("Path Count Full: " + inf);		
			for (int d : depths) {
				List<Result> tempRes = new ArrayList<Result>();
				for (long sDS : seedsDataset) {
					Pair<SingleDTGraph, List<Double>> p = cache.get(sDS).get(inf).get(d);
					SingleDTGraph data = p.getFirst();
					target = p.getSecond();

					List<DTGraph<String,String>> graphs = RDFUtils.getSubGraphs(data.getGraph(), data.getInstances(), d);

					double avgNodes = 0;
					double avgLinks = 0;

					for (DTGraph<String,String> g : graphs){
						avgNodes += g.nodes().size();
						avgLinks += g.links().size();
					}
					avgNodes /= graphs.size();
					avgLinks /= graphs.size();

					System.out.println("Avg # nodes: " + avgNodes + " , avg # links: " + avgLinks);

					List<PathCountKernel> kernels = new ArrayList<PathCountKernel>();

					for (int dd : pathDepths) {
						PathCountKernel kernel = new PathCountKernel(dd, true);			
						kernels.add(kernel);
					}

					//resTable.newRow(kernels.get(0).getLabel() + "_" + inf);
					SimpleGraphFeatureVectorKernelExperiment<GraphList<DTGraph<String,String>>> exp2 = new SimpleGraphFeatureVectorKernelExperiment<GraphList<DTGraph<String,String>>>(kernels, new GraphList<DTGraph<String,String>>(graphs), target, svmParms, seeds, evalFuncs);

					//System.out.println(kernels.get(0).getLabel());
					exp2.run();
					
					if (tempRes.isEmpty()) {
						for (Result res : exp2.getResults()) {
							tempRes.add(res);
						}
					} else {
						for (int i = 0; i < tempRes.size(); i++) {
							tempRes.get(i).addResult(exp2.getResults().get(i));
						}
					}
				}
				for (Result res : tempRes) {
					resTable.addResult(res);
				}
				System.out.println(resTable);
			}
		}
		//*/



		resTable.addCompResults(resTable.getBestResults());
		System.out.println(resTable);


	}

	private static Map<Long, Map<Boolean, Map<Integer,Pair<SingleDTGraph, List<Double>>>>> createDataSetCache(LargeClassificationDataSet data, long[] seeds, double fraction, int minSize, int maxClasses, int[] depths, boolean[] inference) {
		Map<Long, Map<Boolean, Map<Integer,Pair<SingleDTGraph, List<Double>>>>> cache = new HashMap<Long, Map<Boolean, Map<Integer,Pair<SingleDTGraph, List<Double>>>>>();

		for (long seed : seeds) {
			cache.put(seed, new HashMap<Boolean, Map<Integer,Pair<SingleDTGraph, List<Double>>>>());
			data.createSubSet(seed, fraction, minSize, maxClasses);

			for (boolean inf : inference) {
				cache.get(seed).put(inf, new HashMap<Integer,Pair<SingleDTGraph, List<Double>>>());

				for (int depth : depths) {
					Set<Statement> stmts = RDFUtils.getStatements4Depth(tripleStore, data.getRDFData().getInstances(), depth, inf);
					stmts.removeAll(data.getRDFData().getBlackList());
					List<DTNode<String,String>> instanceNodes = new ArrayList<DTNode<String,String>>();
					DTGraph<String,String> graph = RDFUtils.statements2Graph(stmts, RDFUtils.REGULAR_LITERALS, data.getRDFData().getInstances(), instanceNodes, true);
					SingleDTGraph g = new SingleDTGraph(graph, instanceNodes);

					cache.get(seed).get(inf).put(depth, new Pair<SingleDTGraph,List<Double>>(g, new ArrayList<Double>(data.getTarget())));
				}
			}
		}
		return cache;
	}


	private static void createAMDataSet(long seed, int subsetSize, int minSize) {

		Random rand = new Random(seed);

		List<Statement> stmts = tripleStore.getStatementsFromStrings(null, "http://purl.org/collections/nl/am/objectCategory", null);

		System.out.println(tripleStore.getLabel() + " # objects: " + stmts.size());

		instances = new ArrayList<Resource>();
		labels = new ArrayList<Value>();
		blackList = new ArrayList<Statement>();

		for (Statement stmt : stmts) {
			instances.add(stmt.getSubject());
			labels.add(stmt.getObject());
		}

		//		
		//		
		blackList = DataSetUtils.createBlacklist(tripleStore, instances, labels);
		//System.out.println(EvaluationUtils.computeClassCounts(target));

		Collections.shuffle(instances, new Random(seed));
		Collections.shuffle(labels, new Random(seed));

		instances = instances.subList(0, subsetSize);
		labels = labels.subList(0, subsetSize);

		EvaluationUtils.removeSmallClasses(instances, labels, minSize);
		target = EvaluationUtils.createTarget(labels);

		System.out.println("Subset class count: " + EvaluationUtils.computeClassCounts(target));
	}




}
