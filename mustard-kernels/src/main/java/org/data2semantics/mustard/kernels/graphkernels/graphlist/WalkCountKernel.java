package org.data2semantics.mustard.kernels.graphkernels.graphlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.data2semantics.mustard.kernels.ComputationTimeTracker;
import org.data2semantics.mustard.kernels.FeatureInspector;
import org.data2semantics.mustard.kernels.KernelUtils;
import org.data2semantics.mustard.kernels.SparseVector;
import org.data2semantics.mustard.kernels.data.GraphList;
import org.data2semantics.mustard.kernels.graphkernels.FeatureVectorKernel;
import org.data2semantics.mustard.kernels.graphkernels.GraphKernel;
import org.data2semantics.mustard.utils.WalkCountUtils;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.LightDTGraph;


/**
 * Implementation of the Walk Count kernel, which counts all the walks up to the given pathLength in the graphs.
 * Note that both links and nodes count towards the path length. 
 * <ul>
 * <li> pathLength=0, is only 1 node or link,
 * <li> pathLengh=1, is a path with 1 node and 1 link,
 * <li> pathLength=2, is a path with 1 node and 2 links or 2 links and 1 node, 
 * <li> etc...
 * </ul>
 * 
 * @author Gerben 
 */
public class WalkCountKernel implements GraphKernel<GraphList<DTGraph<String,String>>>, FeatureVectorKernel<GraphList<DTGraph<String,String>>>, ComputationTimeTracker, FeatureInspector {
	private int pathLength;
	private long compTime;
	protected boolean normalize;
	private Map<String, Integer> pathDict;
	private Map<String, Integer> labelDict;
	private Map<Integer, String> reversePathDict;
	private Map<Integer, String> reverseLabelDict;


	public WalkCountKernel(int pathLength, boolean normalize) {
		this.normalize = normalize;
		this.pathLength = pathLength;
	}	

	public WalkCountKernel(int depth) {
		this(depth, true);
	}

	public String getLabel() {
		return KernelUtils.createLabel(this);		
	}

	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}

	public long getComputationTime() {
		return compTime;
	}

	public SparseVector[] computeFeatureVectors(GraphList<DTGraph<String,String>> data) {
		pathDict  = new HashMap<String,Integer>();
		labelDict = new HashMap<String,Integer>();
		List<DTGraph<String,String>> graphs = copyGraphs(data.getGraphs());

		// Initialize and compute the featureVectors
		SparseVector[] featureVectors = new SparseVector[graphs.size()];
		for (int i = 0; i < featureVectors.length; i++) {
			featureVectors[i] = new SparseVector();
		}
		
		long tic = System.currentTimeMillis();
		
		for (int i = 0; i < featureVectors.length; i++) {
			for (DTNode<String,String> v : graphs.get(i).nodes()) {
				countPathRec(featureVectors[i], v, "", pathLength);
			}

			for (DTLink<String,String> e : graphs.get(i).links()) {
				countPathRec(featureVectors[i], e, "", pathLength);
			}
		}

		// Set the correct last index
		for (SparseVector fv : featureVectors) {
			fv.setLastIndex(pathDict.size()-1);
		}
		
		compTime = System.currentTimeMillis() - tic;
		
		reversePathDict = new HashMap<Integer,String>();	
		for (String key : pathDict.keySet()) {
			reversePathDict.put(pathDict.get(key), key);
		}
		
		reverseLabelDict = new HashMap<Integer,String>();	
		for (String key : labelDict.keySet()) {
			reverseLabelDict.put(labelDict.get(key), key);
		}

		if (normalize) {
			featureVectors = KernelUtils.normalize(featureVectors);
		}
		return featureVectors;
	}

	public double[][] compute(GraphList<DTGraph<String,String>> data) {
		double[][] kernel = KernelUtils.initMatrix(data.getGraphs().size(), data.getGraphs().size());
		kernel = KernelUtils.computeKernelMatrix(computeFeatureVectors(data), kernel);				
		return kernel;
	}

	private void countPathRec(SparseVector fv, DTNode<String,String> vertex, String path, int depth) {
		// Count path
		path = path + vertex.label();

		if (!pathDict.containsKey(path)) {
			pathDict.put(path, pathDict.size());
		}
		fv.setValue(pathDict.get(path), fv.getValue(pathDict.get(path)) + 1);

		if (depth > 0) {
			for (DTLink<String,String> edge : vertex.linksOut()) {
				countPathRec(fv, edge, path, depth-1);
			}
		}	
	}

	private void countPathRec(SparseVector fv, DTLink<String,String> edge, String path, int depth) {
		// Count path
		path = path + edge.tag();

		if (!pathDict.containsKey(path)) {
			pathDict.put(path, pathDict.size());
		}
		fv.setValue(pathDict.get(path), fv.getValue(pathDict.get(path)) + 1);

		if (depth > 0) {
			countPathRec(fv, edge.to(), path, depth-1);
		}	
	}



	private List<DTGraph<String,String>> copyGraphs(List<DTGraph<String,String>> oldGraphs) {
		List<DTGraph<String,String>> newGraphs = new ArrayList<DTGraph<String,String>>();	

		for (DTGraph<String,String> graph : oldGraphs) {
			LightDTGraph<String,String> newGraph = new LightDTGraph<String,String>();
			for (DTNode<String,String> vertex : graph.nodes()) {
				if (!labelDict.containsKey(vertex.label())) {
					labelDict.put(vertex.label(), labelDict.size());
				}
				String lab = "_" + Integer.toString(labelDict.get(vertex.label()));

				newGraph.add(lab);
			}
			for (DTLink<String,String> edge : graph.links()) {
				if (!labelDict.containsKey(edge.tag())) {
					labelDict.put(edge.tag(), labelDict.size());
				}
				String lab = "_" + Integer.toString(labelDict.get(edge.tag()));

				newGraph.nodes().get(edge.from().index()).connect(newGraph.nodes().get(edge.to().index()), lab); // ?
			}
			newGraphs.add(newGraph);
		}
		return newGraphs;
	}

	public List<String> getFeatureDescriptions(List<Integer> indicesSV) {
		if (labelDict == null) {
			throw new RuntimeException("Should run computeFeatureVectors first");
		} else {
			List<String> desc = new ArrayList<String>();
			
			for (int index : indicesSV) {
				desc.add(WalkCountUtils.getFeatureDecription(reverseLabelDict, reversePathDict, index));
			}
			return desc;
		}
	}
	
}
