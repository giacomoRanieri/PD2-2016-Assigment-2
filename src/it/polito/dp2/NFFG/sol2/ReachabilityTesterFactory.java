package it.polito.dp2.NFFG.sol2;

import it.polito.dp2.NFFG.NffgVerifier;
import it.polito.dp2.NFFG.NffgVerifierException;
import it.polito.dp2.NFFG.NffgVerifierFactory;
import it.polito.dp2.NFFG.lab2.ReachabilityTester;
import it.polito.dp2.NFFG.lab2.ReachabilityTesterException;
import it.polito.dp2.NFFG.sol2.jaxrs.Localhost_Neo4JXMLRest;
import it.polito.dp2.NFFG.sol2.jaxrs.Localhost_Neo4JXMLRest.Resource;

import com.sun.jersey.api.client.Client;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by giaco on 23/12/2016.
 */
public class ReachabilityTesterFactory extends it.polito.dp2.NFFG.lab2.ReachabilityTesterFactory {



	@Override
	public ReachabilityTester newReachabilityTester() throws ReachabilityTesterException {
		NffgVerifier verifier;
		Resource resource;
		try {
			NffgVerifierFactory factory = NffgVerifierFactory.newInstance();
			verifier = factory.newNffgVerifier();
		} catch (NffgVerifierException ex){
			throw new ReachabilityTesterException("Unable to load nffg verifier");
		}

		String URL = System.getProperty("it.polito.dp2.NFFG.lab2.URL");
		Client client = Localhost_Neo4JXMLRest.createClient();
		try {
			resource = Localhost_Neo4JXMLRest.resource(client, new URI(URL));
		}catch (URISyntaxException ex){
			throw new ReachabilityTesterException("Service URL is not a valid URL");
		}

		return new ReachabilityTesterImpl(verifier,resource);
	}
}
