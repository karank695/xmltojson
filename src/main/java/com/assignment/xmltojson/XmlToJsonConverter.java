package com.assignment.xmltojson;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class XmlToJsonConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlToJsonConverter.class);

    /**
     * Converts XML input to JSON and adds a custom field "MatchSummary.TotalMatchScore"
     *
     * @param xmlInput XML data as a String
     * @return JSON String with additional field for total score
     * @throws IOException if an error occurs during processing
     */
    public static String convertXmlToJson(String xmlInput) throws IOException {
    	try {
            LOGGER.info("Starting XML to JSON conversion.");
            XmlMapper xmlMapper = new XmlMapper();
            ObjectMapper jsonMapper = new ObjectMapper();
            JsonNode rootNode = xmlMapper.readTree(xmlInput.getBytes());
            BigInteger totalMatchScore = calculateTotalMatchScore(rootNode);
           
            // Add the new custom field inside MatchSummary
            ObjectNode resultBlockNode = (ObjectNode) rootNode.path("ResultBlock");
            if (!resultBlockNode.has("MatchSummary")) {
                resultBlockNode.putObject("MatchSummary");
            }
            ((ObjectNode) resultBlockNode.path("MatchSummary")).put("TotalMatchScore", totalMatchScore.toString());
           
            // Wrap inside a Response object
            ObjectNode responseNode = jsonMapper.createObjectNode();
            responseNode.set("Response", rootNode);

            // Convert back to JSON String
            String jsonOutput = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseNode);
            LOGGER.info("XML to JSON conversion completed successfully.");
            return jsonOutput;

        } catch (Exception e) {
            LOGGER.error("Error during XML to JSON conversion: {}", e.getMessage(), e);
            throw new IOException("Failed to convert XML to JSON.", e);
        }
    }

    /**
     * Calculates the sum of all scores from the MatchDetails section.
     *
     * @param rootNode Root JSON node obtained from XML
     * @return Sum of scores
     */
    private static BigInteger calculateTotalMatchScore(JsonNode rootNode) {
        BigInteger sum = BigInteger.ZERO;
        try {
            JsonNode matchDetailsNode = rootNode.path("ResultBlock").path("MatchDetails");

            if (matchDetailsNode.isMissingNode()) {
                LOGGER.warn("MatchDetails node is missing in the XML.");
                return sum;
            }

            // Handle multiple Match nodes
            Iterator<JsonNode> matches = matchDetailsNode.elements();
            while (matches.hasNext()) {
                JsonNode match = matches.next().path("Match");
                if (match.isArray()) {
                    for (JsonNode node : match) {
                        sum = addScore(sum, node.path("Score").asText());
                    }
                } else {
                    sum = addScore(sum, match.path("Score").asText());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while calculating total match score: {}", e.getMessage(), e);
        }
        return sum;
    }

    /**
     * Adds score to the total, ensuring numeric validation.
     *
     * @param total    Current total sum
     * @param scoreStr Score value as String
     * @return Updated total
     */
    private static BigInteger addScore(BigInteger total, String scoreStr) {
        try {
            return total.add(new BigInteger(scoreStr));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid score value encountered: {}. Skipping...", scoreStr);
            return total;
        }
    }

    public static void main(String[] args) {
    	 String xmlInput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    	            "<Response>\n" +
    	            "    <ResultBlock>\n" +
    	            "        <ErrorWarnings>\n" +
    	            "            <Errors errorCount=\"0\" />\n" +
    	            "            <Warnings warningCount=\"1\">\n" +
    	            "                <Warning>\n" +
    	            "                    <Number>102001</Number>\n" +
    	            "                    <Message>Minor mismatch in address</Message>\n" +
    	            "                    <Values>\n" +
    	            "                        <Value>Bellandur</Value>\n" +
    	            "                        <Value>Bangalore</Value>\n" +
    	            "                    </Values>\n" +
    	            "                </Warning>\n" +
    	            "            </Warnings>\n" +
    	            "        </ErrorWarnings>\n" +
    	            "        <MatchDetails>\n" +
    	            "            <Match>\n" +
    	            "                <Entity>John</Entity>\n" +
    	            "                <MatchType>Exact</MatchType>\n" +
    	            "                <Score>35</Score>\n" +
    	            "            </Match>\n" +
    	            "            <Match>\n" +
    	            "                <Entity>Doe</Entity>\n" +
    	            "                <MatchType>Exact</MatchType>\n" +
    	            "                <Score>50</Score>\n" +
    	            "            </Match>\n" +
    	            "        </MatchDetails>\n" +
    	            "        <API>\n" +
    	            "            <RetStatus>SUCCESS</RetStatus>\n" +
    	            "            <ErrorMessage />\n" +
    	            "            <SysErrorCode />\n" +
    	            "            <SysErrorMessage />\n" +
    	            "        </API>\n" +
    	            "    </ResultBlock>\n" +
    	            "</Response>";

        try {
            String jsonOutput = XmlToJsonConverter.convertXmlToJson(xmlInput);
            System.out.println(jsonOutput);
        } catch (IOException e) {
            LOGGER.error("Conversion failed: {}", e.getMessage(), e);
        }
    }
}
