package no.uib.pap.pathfinder.model.graph;

/**
 * Simple representation of a graph.
 *
 * @author Marc Vaudel
 */
public class Graph {
    
    /**
     * Array of vertices.
     */
    public final Vertex[] vertices;
    
    /**
     * Constructor.
     * 
     * @param vertices Array of vertices
     */
    public Graph(Vertex[] vertices) {
        
        this.vertices = vertices;
    
    }

}
