package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.el.parser.ParseException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GeographyService {
    public Map<String, Set<String>> fetchStatesWithUniqueCountiesService() {
        Map<String, Set<String>> stateCounties = new HashMap<>();
        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/hcb_o");

        // Separate connection to a repository
        RepositoryConnection connection = repository.getConnection();

        try {
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL,
                    "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/>" +
                            "PREFIX tsn: <http://purl.org/net/tsn#>" +
                            "SELECT DISTINCT ?countyName ?name " +
                            "WHERE {" +
                                "?subject tsn:hasName ?name  ." +
                                "FILTER (!CONTAINS(STR(?name), '_') && CONTAINS(STR(?subject), 'State'))" +
                                "OPTIONAL {" +
                                    "?countyversion sem:isPartOf ?subject ;" +
                                    "tsn:hasName ?countyversionName;" +
                                    "tsn:isVersionOfCounty ?county." +
                                    "?county tsn:hasName ?countyName" +
                                "}" +
                            "}" +
                            "ORDER BY ?name ?countyName");
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    String stateName = Optional.ofNullable(bindingSet.getValue("name")).map(Value::stringValue).orElse(null);
                    if (stateName != null) {
                        Set<String> counties = stateCounties.computeIfAbsent(stateName, k -> new HashSet<>());
                        Optional.ofNullable(bindingSet.getValue("countyName"))
                                .map(Value::stringValue)
                                .ifPresent(counties::add);
                    }
                }

                stateCounties.forEach((state, counties) -> {
                    System.out.println("Processing state: " + state + " with counties: " + counties);
                });
            }

        }catch (Exception e) {
                // Proper error handling
                e.printStackTrace();
        } finally {
            connection.close();
        }
        return stateCounties;
    }

    public String getGeoJsonForLocation(String date, String location, String locationType) {
        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/hcb_o");
        RepositoryConnection connection = repository.getConnection();

        try {
            String queryString = String.format(
                    "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                            "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/>\n" +
                            "PREFIX tsn: <http://purl.org/net/tsn#>\n" +
                            "SELECT ?geometry\n" +
                            "WHERE {\n" +
                            "  ?location tsn:hasName \"%s\" ;\n" +
                            "            geo:asWKT ?geometry ;\n" +
                            "            sem:hasTimeStamp \"%s\" .\n" +
                            "  FILTER(CONTAINS(STR(?location), \"%s\"))\n" +
                            "}", location, date, locationType.equals("state") ? "State" : "County");

            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

            try (TupleQueryResult result = query.evaluate()) {
                if (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Value geometryValue = bindingSet.getValue("geometry");

                    if (geometryValue != null) {
                        return convertWktToGeoJson(geometryValue.stringValue());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return null;
    }

    private String convertWktToGeoJson(String wkt) throws ParseException, JsonProcessingException, org.locationtech.jts.io.ParseException {
        if (wkt == null || wkt.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty WKT input.");
        }

        // Remove any CRS identifier from the start of the WKT, if present
        if (wkt.startsWith("<http://www.opengis.net/def/crs/")) {
            int endIndex = wkt.indexOf(">") + 1;
            wkt = wkt.substring(endIndex).trim();
        }

        WKTReader reader = new WKTReader();
        Geometry geometry = null;

        geometry = reader.read(wkt);

        if (geometry == null) {
            throw new IllegalStateException("Failed to convert WKT to Geometry.");
        }

        try {
            GeoJsonWriter writer = new GeoJsonWriter();
            return writer.write(geometry);
        } catch (UnsupportedOperationException e) {
            // Handle the case where a method or operation is not supported
            System.err.println("Operation not supported: " + e.getMessage());
            throw new IllegalStateException("Conversion operation failed.", e);
        }
    }
}
