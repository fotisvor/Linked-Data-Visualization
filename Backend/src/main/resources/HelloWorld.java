//import com.bedatadriven.jackson.datatype.jts.JtsModule;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.el.parser.ParseException;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello World app for GraphDB
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
public class HelloWorld {
    @GetMapping("/geojson")
    public void hello() throws Exception {
        // Connect to a remote repository using the GraphDB client API
        // (ruleset is irrelevant for this example)
        //GraphDBHTTPRepository repository = new GraphDBHTTPRepositoryBuilder()
                //.withServerUrl("http://localhost:7200")
                //.withRepositoryId("hcb_o")
                //.withCluster(); // uncomment this line to enable cluster mode
                //.build();

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
                            "  <http://time-space-event.com/resource/Alaska/County/ALEUTIANS_WEST_2/Geometry/7> geo:asWKT ?geometry .\n" +
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
                    System.out.println("GeoJSON Data: " + geoJson);
                }
            }

            tupleQueryResult.close();
        } finally {
            // It is best to close the connection in a finally block
            connection.close();
        }
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

    public static void main(String[] args) throws Exception {
        new HelloWorld().hello();
    }
}
