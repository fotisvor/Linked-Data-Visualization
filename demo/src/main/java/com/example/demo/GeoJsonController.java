package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.el.parser.ParseException;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
public class GeoJsonController {
    @Autowired
    private GeographyService geographyService;

    @GetMapping("/states")
    public List<State> fetchStatesWithUniqueCounties() {

        Map<String, Set<String>> stateCounties = this.geographyService.fetchStatesWithUniqueCountiesService();
        List<State> states = new ArrayList<>();
        stateCounties.forEach((state, counties) -> {
            State stateObj = new State();
            stateObj.setName(state);
            stateObj.setCounties(new ArrayList<>(counties));
            states.add(stateObj);
        });
        return states;
    }


    @GetMapping("/geojson")
    public String getGeoJson() throws ParseException, org.locationtech.jts.io.ParseException, JsonProcessingException {
        // Alternative access to a remote repository using pure RDF4J
        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/hcb_o");

        // Separate connection to a repository
        RepositoryConnection connection = repository.getConnection();

        try {
            TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL,
                    "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                            "\n" +
                            "SELECT ?geometry\n" +
                            "WHERE {\n" +
                            "  <http://time-space-event.com/resource/Alabama/County/PICKENS_1/Geometry/690> geo:asWKT ?geometry .\n" +
                            "}");

            TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
            // Initialize a variable to hold your WKT data
            String wktData = "";

            if (tupleQueryResult.hasNext()) {
                BindingSet bindingSet = tupleQueryResult.next();
                Binding geometryBinding = bindingSet.getBinding("geometry");

                if (geometryBinding != null) {
                    wktData = geometryBinding.getValue().stringValue();
                    //System.out.println("WKT Data: " + wktData);

                    // Convert WKT to GeoJSON
                    String geoJson = convertWktToGeoJson(wktData);
                    return geoJson;
                }
            }

            tupleQueryResult.close();
        } finally {
            // It is best to close the connection in a finally block
            connection.close();
        }
        return null;
    }

    @GetMapping("/search")
    public String getGeoJsonForLocation(@RequestParam String selectedLocation,
                                        @RequestParam String locationType,
                                        @RequestParam String date) throws ParseException, org.locationtech.jts.io.ParseException, JsonProcessingException {
        // Alternative access to a remote repository using pure RDF4J
        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/hcb_o");
        System.out.println("params " + selectedLocation + locationType + date);
        // Separate connection to a repository
        RepositoryConnection connection = repository.getConnection();

        try {
            String queryStr;
            if (locationType.equals("state")) {
                queryStr = String.format(
                        "PREFIX hcb: <https://hcbkg.l3s.uni-hannover.de/ontology/>\n" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                                "PREFIX tsn: <http://purl.org/net/tsn#>\n" +
                                "PREFIX tsnchange: <http://purl.org/net/tsnchange#>\n" +
                                "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                                "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                                "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/>\n" +
                                "SELECT DISTINCT ?x3 { \n" +
                                "  ?x0 a geo:Geometry .\n" +
                                "  ?x1 a hcb:StateVersion .\n" +
                                "  ?x1 tsn:referencePeriod ?x2 .\n" +
                                "  ?x2 time:hasBeginning ?x4 .\n" +
                                "  OPTIONAL { ?x2 time:hasEnd ?x8 . ?x8 time:inXSDDate ?x7 . }\n" +
                                "  ?x4 time:inXSDDate ?x5 .\n" +

                                "  ?x1 geo:hasGeometry ?x0 .\n" +
                                "  ?x0 geo:asWKT ?x3 .\n" +
                                "  ?state a hcb:State .\n" +
                                "  ?state tsn:hasName \"%s\" .\n" +
                                "  ?x1 tsn:isVersionOfState ?state .\n" +

                                "  FILTER(?x5 <= \"%s\"^^xsd:date && (!bound(?x7) || ?x7 >= \"%s\"^^xsd:date)) .\n" +
                                "}", selectedLocation, date, date);
            }else {
                queryStr = String.format(
                        "PREFIX hcb: <https://hcbkg.l3s.uni-hannover.de/ontology/>\n" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                                "PREFIX tsn: <http://purl.org/net/tsn#>\n" +
                                "PREFIX tsnchange: <http://purl.org/net/tsnchange#>\n" +
                                "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                                "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                                "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/>\n" +
                                "SELECT DISTINCT ?x3 { \n" +
                                "  ?x0 a geo:Geometry .\n" +
                                "  ?x1 a hcb:CountyVersion .\n" +
                                "  ?x1 tsn:referencePeriod ?x2 .\n" +
                                "  ?x2 time:hasBeginning ?x4 .\n" +
                                "  OPTIONAL { ?x2 time:hasEnd ?x8 . ?x8 time:inXSDDate ?x7 . }\n" +
                                "  ?x4 time:inXSDDate ?x5 .\n" +

                                "  ?x1 geo:hasGeometry ?x0 .\n" +
                                "  ?x0 geo:asWKT ?x3 .\n" +
                                "  ?county tsn:hasName \"%s\" .\n" +
                                "  ?county tsn:hasCountyVersion ?x1 .\n" +
                                "  ?state a hcb:State .\n" +
                                "  ?state tsn:hasName  \"%s\" .\n" +
                                "  ?stateversion tsn:isVersionOfState ?state .\n" +
                                "  ?x1 sem:isPartOf ?stateversion .\n" +
                                "  FILTER(?x5 <= \"%s\"^^xsd:date &&  (?x7 >= \"%s\"^^xsd:date || !EXISTS { ?x2 time:hasEnd ?x8 })) .\n" +
                                "}", selectedLocation, locationType, date, date);


            }
            TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);

            TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
            // Initialize a variable to hold your WKT data
            String wktData = "";

            if (tupleQueryResult.hasNext()) {
                BindingSet bindingSet = tupleQueryResult.next();
                Binding geometryBinding = bindingSet.getBinding("x3");

                if (geometryBinding != null) {
                    wktData = geometryBinding.getValue().stringValue();
                    System.out.println("WKT Data: " + wktData);
                    // Convert WKT to GeoJSON
                    return convertWktToGeoJson(wktData);
                }

            }else{
                System.out.println("empty");
            }

            tupleQueryResult.close();
        } finally {
            // It is best to close the connection in a finally block
            connection.close();
        }
        return null;
    }

    @GetMapping("/previousCountyVersion")
    public String getPreviousCountyVersion(@RequestParam String selectedLocation, @RequestParam String locationType, @RequestParam String date) throws Exception {
        String previousWkt;
        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/hcb_o");
        RepositoryConnection connection = repository.getConnection();

        try {

            if (!locationType.equals("state")) {
                previousWkt = String.format(
                    "PREFIX hcb: <https://hcbkg.l3s.uni-hannover.de/ontology/> " +
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                            "PREFIX tsn: <http://purl.org/net/tsn#> " +
                            "PREFIX geo: <http://www.opengis.net/ont/geosparql#> " +
                            "PREFIX time: <http://www.w3.org/2006/time#> " +
                            "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/> " +
                            "PREFIX ant: <https://hcbkg.l3s.uni-hannover.de/ontology/> " +
                            "SELECT DISTINCT ?x3  { " +
                            "  ?x0 a geo:Geometry . " +
                            "  ?x1 a hcb:CountyVersion . " +
                            "  ?previousVersion a hcb:CountyVersion . " +
                            "  ?x1 tsn:referencePeriod ?x2 . " +
                            "  ?x2 time:hasBeginning ?x4 . " +
                            "  ?x2 time:hasEnd ?x8 . " +
                            "  ?x4 time:inXSDDate ?x5 . " +
                            "  ?x8 time:inXSDDate ?x7 . " +
                            "  ?county tsn:hasName \"%s\" . " +
                            "  ?county tsn:hasCountyVersion ?x1 . " +
                            "  ?state a hcb:State . " +
                            "  ?state tsn:hasName \"%s\" . " +
                            "  ?stateversion tsn:isVersionOfState ?state . " +
                            "  ?x1 sem:isPartOf ?stateversion . " +
                            "  ?x1 ant:hasPreviousCountyVersion ?previousVersion . " +
                            "  ?previousVersion geo:hasGeometry ?x0 . " +
                            "  ?x0 geo:asWKT ?x3 . " +
                            "  FILTER(?x5 <= \"%s\"^^xsd:date && ?x7 >= \"%s\"^^xsd:date) . " +
                            "}", selectedLocation,locationType, date, date);
            }else {
                previousWkt = String.format(
                        "PREFIX hcb: <https://hcbkg.l3s.uni-hannover.de/ontology/> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "PREFIX tsn: <http://purl.org/net/tsn#> " +
                                "PREFIX geo: <http://www.opengis.net/ont/geosparql#> " +
                                "PREFIX time: <http://www.w3.org/2006/time#> " +
                                "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/> " +
                                "PREFIX ant: <https://hcbkg.l3s.uni-hannover.de/ontology/> " +
                                "SELECT DISTINCT ?x3  { " +
                                "  ?x0 a geo:Geometry . " +
                                "  ?x1 a hcb:StateVersion . " +
                                "  ?state a hcb:State . " +
                                "  ?previousVersion a hcb:StateVersion . " +
                                "  ?x1 tsn:referencePeriod ?x2 . " +
                                "  ?x2 time:hasBeginning ?x4 . " +
                                "  ?x2 time:hasEnd ?x8 . " +
                                "  ?x4 time:inXSDDate ?x5 . " +
                                "  ?x8 time:inXSDDate ?x7 . " +
                                "  ?state tsn:hasName \"%s\" . " +
                                "  ?state tsn:hasStateVersion ?x1 . " +


                                "  ?x1 ant:hasPreviousStateVersion ?previousVersion . " +
                                "  ?previousVersion geo:hasGeometry ?x0 . " +
                                "  ?x0 geo:asWKT ?x3 . " +
                                "  FILTER(?x5 <= \"%s\"^^xsd:date && ?x7 >= \"%s\"^^xsd:date) . " +
                                "}", selectedLocation, date, date);
            }
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, previousWkt);
            String wktData = "";
            try (TupleQueryResult result = query.evaluate()) {
                if (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Binding geometryBinding = bindingSet.getBinding("x3");

                    if (geometryBinding != null) {
                        wktData = geometryBinding.getValue().stringValue();
                        System.out.println("Previous WKT Data: " + wktData);
                        // Convert WKT to GeoJSON
                        return convertWktToGeoJson(wktData);
                    }

                }else{
                    System.out.println("empty");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return null;

    }

    @GetMapping("/countyMetadata")
    public List<Map<String, String>> getCountyMetadata(
            @RequestParam String countyName,
            @RequestParam String stateName,
            @RequestParam String date) {
        List<Map<String, String>> metadataList = new ArrayList<>();
        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/hcb_o");
        String queryString;
        // Separate connection to a repository
        try (RepositoryConnection connection = repository.getConnection()) {
            if(!stateName.equals("state")) {
                queryString = String.format(
                        "PREFIX hcb: <https://hcbkg.l3s.uni-hannover.de/ontology/>" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                "PREFIX tsn: <http://purl.org/net/tsn#>" +
                                "PREFIX tsnchange: <http://purl.org/net/tsnchange#>" +
                                "PREFIX geo: <http://www.opengis.net/ont/geosparql#>" +
                                "PREFIX time: <http://www.w3.org/2006/time#>" +
                                "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/>" +
                                "PREFIX ant: <http://time-space-event.com/ontology/>" +
                                "PREFIX strdf: <http://strdf.di.uoa.gr/ontology#>" +
                                "SELECT DISTINCT ?countyName ?stateName ?beginning ?end  { " +
                                "  ?countyVersion a hcb:CountyVersion ." +
                                "  ?countyVersion sem:isPartOf ?stateVersion ." +
                                "  ?stateVersion tsn:isVersionOfState ?state ." +
                                "  ?state tsn:hasName ?stateName ." +
                                "  ?countyVersion tsn:hasName ?countyName ." +
                                "  ?countyVersion tsn:referencePeriod ?referencePeriod ." +
                                "  ?referencePeriod time:hasBeginning ?beginningInterval ." +
                                "  ?referencePeriod time:hasEnd ?endInterval ." +
                                "  ?beginningInterval time:inXSDDate ?beginning ." +
                                "  ?endInterval time:inXSDDate ?end ." +
                                "  ?countyVersion geo:hasGeometry ?geometry ." +
                                "  ?geometry geo:asWKT ?wkt ." +
                                "  ?county tsn:hasName \"%s\" ." +
                                "  ?county tsn:hasCountyVersion ?countyVersion ." +
                                "  ?state tsn:hasName \"%s\" ." +
                                "  FILTER(?beginning <= \"%s\"^^xsd:date && ?end >= \"%s\"^^xsd:date) ." +
                                "}", countyName, stateName, date, date);
            }else{
                queryString = String.format(
                        "PREFIX hcb: <https://hcbkg.l3s.uni-hannover.de/ontology/>\n" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                                "PREFIX tsn: <http://purl.org/net/tsn#>\n" +
                                "PREFIX tsnchange: <http://purl.org/net/tsnchange#>\n" +
                                "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                                "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                                "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/>\n" +
                                "SELECT DISTINCT ?countyName ?beginning ?end { \n" +
                                "  ?x0 a geo:Geometry .\n" +
                                "  ?x1 a hcb:StateVersion .\n" +
                                "  ?x1 tsn:referencePeriod ?x2 .\n" +
                                "  ?x2 time:hasBeginning ?x4 .\n" +
                                "  OPTIONAL { ?x2 time:hasEnd ?x8 . ?x8 time:inXSDDate ?end . }\n" +
                                "  ?x4 time:inXSDDate ?beginning .\n" +
                                " ?x1 tsn:hasName ?countyName .\n" +
                                "  ?x1 geo:hasGeometry ?x0 .\n" +
                                "  ?x0 geo:asWKT ?x3 .\n" +
                                "  ?state a hcb:State .\n" +
                                "  ?state tsn:hasName \"%s\" .\n" +
                                "  ?x1 tsn:isVersionOfState ?state .\n" +

                                "  FILTER(?beginning <= \"%s\"^^xsd:date && (!bound(?end) || ?end >= \"%s\"^^xsd:date)) .\n" +
                                "}", countyName, date, date);
            }
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("countyName", bindingSet.getValue("countyName").stringValue());
                    metadata.put("stateName", stateName);
                    metadata.put("beginning", bindingSet.getValue("beginning").stringValue());
                    metadata.put("end", bindingSet.getValue("end").stringValue());
                    metadataList.add(metadata);
                }
            }
        } catch (Exception e) {
            // Proper error handling
            e.printStackTrace();
        }
        return metadataList;

    }

    @GetMapping("/previousCountyMetadata")
    public List<Map<String, String>> getPreviousCountyMetadata(@RequestParam String countyName,
                                                               @RequestParam String stateName,
                                                               @RequestParam String date) throws Exception {

        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/hcb_o");
        RepositoryConnection connection = repository.getConnection();
        List<Map<String, String>> metadataList = new ArrayList<>();
        try {
            String queryString = String.format(
                    "PREFIX hcb: <http://www.semanticweb.org/savtr/ontologies/2022/1/HistoricGeoChanges-23/> " +
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                            "PREFIX tsn: <http://purl.org/net/tsn#> " +
                            "PREFIX geo: <http://www.opengis.net/ont/geosparql#> " +
                            "PREFIX time: <http://www.w3.org/2006/time#> " +
                            "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/> " +
                            "PREFIX ant: <http://time-space-event.com/ontology/> " +
                            "SELECT DISTINCT ?countyName ?stateName ?beginning ?end  { " +
                            "  ?x0 a geo:Geometry . " +
                            "  ?x1 a hcb:CountyVersion . " +
                            "  ?previousVersion a hcb:CountyVersion . " +

                            "  ?x1 tsn:referencePeriod ?referencePeriod . " +
                            "  ?referencePeriod time:hasBeginning ?beginningInterval ." +
                            "  ?referencePeriod time:hasEnd ?endInterval ." +
                            "  ?beginningInterval time:inXSDDate ?beginning ." +
                            "  ?endInterval time:inXSDDate ?end ." +
                            "  ?x4 time:inXSDDate ?x5 . " +
                            "  ?x8 time:inXSDDate ?x7 . " +
                            "  ?county tsn:hasName \"%s\" . " +
                            "  ?county tsn:hasCountyVersion ?x1 . " +

                            "  ?state a hcb:State . " +
                            "  ?state tsn:hasName \"%s\" . " +
                            "  ?stateversion tsn:isVersionOfState ?state . " +
                            "  ?x1 sem:isPartOf ?stateversion . " +
                            "  ?x1 ant:hasPreviousCountyVersion ?previousVersion . " +
                            "  ?previousVersion tsn:hasName ?countyName ." +
                            "  ?previousVersion geo:hasGeometry ?x0 . " +
                            "  ?x0 geo:asWKT ?x3 . " +
                            "  FILTER(?beginning <= \"%s\"^^xsd:date && ?end >= \"%s\"^^xsd:date) . " +
                            "}", countyName, stateName, date, date);

            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("countyName", bindingSet.getValue("countyName").stringValue());
                    metadata.put("stateName", bindingSet.getValue("stateName").stringValue());
                    metadata.put("beginning", bindingSet.getValue("beginning").stringValue());
                    metadata.put("end", bindingSet.getValue("end").stringValue());
                    metadataList.add(metadata);
                }
            }
        } catch (Exception e) {
            // Proper error handling
            e.printStackTrace();
        }
        return metadataList;

    }

    private String convertWktToGeoJson(String wkt) throws ParseException, JsonProcessingException, org.locationtech.jts.io.ParseException {
        if (wkt == null || wkt.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty WKT input.");
        }

        // Remove any CRS identifier from the start of the WKT, if present
        if (wkt.startsWith("<http://www.opengis.net/def/crs/EPSG/0/4326>")) {
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

