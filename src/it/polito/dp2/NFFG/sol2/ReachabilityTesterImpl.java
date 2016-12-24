package it.polito.dp2.NFFG.sol2;

import it.polito.dp2.NFFG.LinkReader;
import it.polito.dp2.NFFG.NffgReader;
import it.polito.dp2.NFFG.NffgVerifier;
import it.polito.dp2.NFFG.NodeReader;
import it.polito.dp2.NFFG.lab2.NoGraphException;
import it.polito.dp2.NFFG.lab2.ReachabilityTester;
import it.polito.dp2.NFFG.lab2.ServiceException;
import it.polito.dp2.NFFG.lab2.UnknownNameException;
import it.polito.dp2.NFFG.sol2.jaxrs.*;
import it.polito.dp2.NFFG.sol2.jaxrs.Localhost_Neo4JXMLRest.Resource;

import javax.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by giaco on 23/12/2016.
 */
public class ReachabilityTesterImpl implements ReachabilityTester {

	private String currentGraphName;
	private NffgVerifier verifier;
	private Resource resource;
	private ObjectFactory objFac;
	private Map<NodeReader, Node> nodeMap;

	public ReachabilityTesterImpl(NffgVerifier verifier, Resource resource) {
		this.verifier = verifier;
		this.resource = resource;
		this.objFac = new ObjectFactory();
	}

	@Override
	public void loadNFFG(String name) throws UnknownNameException, ServiceException {
		currentGraphName = name;
		nodeMap = new HashMap<>();

		String res;
		NffgReader nffg = verifier.getNffg(name);
		if (nffg == null) {
			currentGraphName = null;
			nodeMap = null;
			throw new UnknownNameException("The NFFG with name \"" + name + "\" does not exist.");
		}

		//deleting all nodes
		Resource.Nodes nodesCall = resource.nodes();
		try {
			res = nodesCall.deleteAsXml(String.class);
		} catch (WebApplicationException ex) {
			currentGraphName = null;
			nodeMap = null;
			throw new ServiceException("Unable to delete nodes", ex);
		}

		System.out.println("Delete nodes result: " + res);

		//adding all nodes

		Resource.Node nodeCall = resource.node();

		Set<NodeReader> nodes = nffg.getNodes();
		for (NodeReader noder : nodes) {
			Node node = objFac.createNode();
			List<Property> props = node.getProperty();
			Property nameProp = new Property();
			nameProp.setName("name");
			nameProp.setValue(noder.getName());
			props.add(nameProp);
			try {
				Node resp = nodeCall.postXmlAsNode(node);
				nodeMap.put(noder, resp);
			} catch (WebApplicationException ex) {
				currentGraphName = null;
				nodeMap = null;
				throw new ServiceException("Unable to load node \"" + noder.getName() + "\"", ex);
			}
		}

		//adding all relationship

		for (NodeReader source : nodeMap.keySet()) {
			Node sourceNode = nodeMap.get(source);

			for (LinkReader link : source.getLinks()) {
				NodeReader dest = link.getDestinationNode();
				Node destNode = nodeMap.get(dest);

				Resource.NodeNodeidRelationship putRelationCall = resource.nodeNodeidRelationship(sourceNode.getId());

				Relationship relation = objFac.createRelationship();
				relation.setType("link");
				relation.setDstNode(destNode.getId());
				try {
					putRelationCall.postXmlAsRelationship(relation);
					System.out.println("Link: \"" + source.getName() + "\" and \"" + dest.getName() + "\"");
				} catch (WebApplicationException ex) {
					currentGraphName = null;
					nodeMap = null;
					throw new ServiceException("Unable to load relation between \"" + source.getName() + "\" and \"" + dest.getName() + "\"", ex);
				}
			}

		}


	}

	@Override
	public boolean testReachability(String srcName, String destName) throws UnknownNameException, ServiceException, NoGraphException {
		if (currentGraphName == null) {
			throw new NoGraphException("No graph loaded");
		}

		NodeReader sourceR = verifier.getNffg(currentGraphName).getNode(srcName);
		if (sourceR == null) {
			throw new UnknownNameException("The source node does not exist");
		}

		NodeReader destR = verifier.getNffg(currentGraphName).getNode(destName);
		if (destR == null) {
			throw new UnknownNameException("The destination node does not exist");
		}

		Node sourceNode = nodeMap.get(sourceR);
		Node destNode = nodeMap.get(destR);

		Resource.NodeNodeidPaths nodePathCall = resource.nodeNodeidPaths(sourceNode.getId());

		Paths resp;
		try {
			resp = nodePathCall.getAsPaths(destNode.getId());
		} catch (WebApplicationException ex) {
			throw new ServiceException("Error finding path between \"" + sourceR.getName() + "\" and \"" + destR.getName() + "\"", ex);
		}
		return resp.getPath().size() > 0;
	}

	@Override
	public String getCurrentGraphName() {
		return currentGraphName;
	}
}
