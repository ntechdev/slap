package com.ntechinternational.slap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

@Path("/processrequest")
public class SlapRestImpl {
	
	private static final String INVALID_VISITOR_ID_PROVIDED = "Invalid visitor ID provided";
	private static final String VISITOR_ID = "visitorId";
	private static final String MAP_FILENAME = "Map.xml";
	
	/**
	 * This is the main web service method that processes all the various request and provides a response
	 * @param visitorId the visitor id
	 * @return the string response
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public SlapResponse processRequest(@Context UriInfo uriInfo){
		
		SlapResponse processedResponse = new SlapResponse();;
		
		//if valid visitor id has been provided
		//TODO: check if the test is valid, and meets the requirements
		MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(); 
		long visitorId = -1; //default null value
		try{
			visitorId = Long.parseLong(queryParams.getFirst(VISITOR_ID));
			processedResponse.visitorId = visitorId;
			
			if(visitorId > 0){
				processedResponse = processRequest(visitorId, queryParams);
			}
			else{
				processedResponse.errorDescription = INVALID_VISITOR_ID_PROVIDED;
			}
		}
		catch(NumberFormatException ex){
			processedResponse.visitorId = 0;
			processedResponse.errorDescription = INVALID_VISITOR_ID_PROVIDED;
		}
		catch(Exception ex){
			processedResponse.errorDescription = ex.getMessage();
		}
		
		return processedResponse;
		
		
	}

	private SlapResponse processRequest(long visitorId,
		MultivaluedMap<String, String> queryParams) throws Exception {
		SlapResponse response = new SlapResponse();
		
		//Step 1: Validate the token
		@SuppressWarnings("unused")
		Token token = new TokenValidator().checkTokenId(visitorId);
		
		//Step 2: Load the configuration information from the map.xml file
		ConfigurationMap configDetails = ConfigurationMap.getConfig(MAP_FILENAME);
		
		//Step 3: Prepare Server Query and fetch response from server
		//TODO: parallelize question and challenge response
		String questionResponse = new QueryManager().query(visitorId, queryParams, configDetails, "/questionresponse-1.xml");
		
		String challengeResponse = new QueryManager().query(visitorId, queryParams, configDetails, "/challengeresponse-1.xml");
		
		//Step 4: Merge the response and return the response
		
		response.questions = XmlParser.transformDoc(questionResponse, configDetails.backendDocNode, configDetails.responseMappings);
		response.items = XmlParser.transformDoc(challengeResponse, configDetails.backendDocNode, configDetails.responseMappings);
		response.visitorId = visitorId;
		
		return response;
	}

	
	
}
