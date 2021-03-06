package net.optim.jswarm.pso;

import java.awt.Color;
import java.awt.Graphics;
import net.optim.jswarm.pso.variables.VariablesUpdate;
import net.optim.jswarm.pso.particle.SimpleParticleUpdate;
import net.optim.jswarm.pso.particle.ParticleUpdate;
import net.optim.jswarm.pso.neighbor.Neighborhood;
import net.optim.jswarm.FitnessFunction;
import net.optim.jswarm.Particle;
import net.optim.jswarm.pso.constraints.ConstraintsHandler;
import net.optim.jswarm.pso.constraints.NearestBoundary;
import net.optim.jswarm.pso.init.GenericInitialization;
import net.optim.jswarm.pso.init.UniformInitialization;
import net.optim.jswarm.pso.variables.SimpleVariablesUpdate;

/**
 * A swarm of particles
 * @author Paulius Danėnas <danpaulius@gmail.com>, original code by Pablo Cingolani <pcingola@users.sourceforge.net>
 */
public class Swarm implements Cloneable {

    public static double DEFAULT_GLOBAL_INCREMENT = 0.9;
    public static double DEFAULT_INERTIA = 0.95;
    public static int DEFAULT_NUMBER_OF_PARTICLES = 25;
    public static double DEFAULT_PARTICLE_INCREMENT = 0.9;
    public static double VELOCITY_GRAPH_FACTOR = 10.0;

    /** Best fitness so far (global best) */
    protected double bestFitness;
    /** Index of best particle so far */
    protected int bestParticleIndex;
    /** Best position so far (global best) */
    protected double bestPosition[];
    /** Fitness function for this swarm */
    protected FitnessFunction fitnessFunction;
    /** Global increment (for velocity update), usually called 'c2' constant */
    protected double globalIncrement;
    /** Inertia (for velocity update), usually called 'w' constant */
    protected double inertia;
    /** Maximum position (for each dimension) */
    protected double maxPosition[];
    /** Maximum Velocity (for each dimension) */
    protected double maxVelocity[];
    /** Minimum position (for each dimension) */
    protected double minPosition[];
    /** Minimum Velocity for each dimension. WARNING: Velocity is no in Abs value (so setting minVelocity to 0 is NOT correct!) */
    protected double minVelocity[];
    /** How many times 'particle.evaluate()' has been called? */
    protected int numEvaluations;
    /** Number of particles in this swarm */
    protected int numberOfParticles;
    /** Particle's increment (for velocity update), usually called 'c1' constant */
    protected double particleIncrement;
    /** Particles in this swarm */
    protected Particle particles[];
    /** Particle update strategy */
    protected ParticleUpdate particleUpdate;
    /** A sample particles: Build other particles based on this one */
    protected Particle sampleParticle;
    /** Variables update */
    protected VariablesUpdate variablesUpdate;
    /** Neighborhood */
    protected Neighborhood neighborhood;
    /** Neighborhood increment (for velocity update), usually called 'c3' constant */
    protected double neighborhoodIncrement;
    /* Constraints handler */
    protected ConstraintsHandler constraintHandler;
    /* Particle initial value initialization */
    protected GenericInitialization initializer;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
    /**
     * Create a Swarm and set default values
     * @param numberOfParticles : Number of particles in this swarm (should be greater than 0).
     *                          If unsure about this parameter, try Swarm.DEFAULT_NUMBER_OF_PARTICLES or greater
     * @param sampleParticle    : A particle that is a sample to build all other particles
     * @param fitnessFunction   : Fitness function used to evaluate each particle
     */
    public Swarm(int numberOfParticles, Particle sampleParticle, FitnessFunction fitnessFunction) {
        if (sampleParticle == null)
            throw new RuntimeException("Sample particle can't be null!");
        if (numberOfParticles <= 0)
            throw new RuntimeException("Number of particles should be greater than zero.");

        globalIncrement = DEFAULT_GLOBAL_INCREMENT;
        inertia = DEFAULT_INERTIA;
        particleIncrement = DEFAULT_PARTICLE_INCREMENT;
        numEvaluations = 0;
        this.numberOfParticles = numberOfParticles;
        this.sampleParticle = sampleParticle;
        this.fitnessFunction = fitnessFunction;
        bestFitness = Double.NaN;
        bestParticleIndex = -1;

        // Set up particle update strategy (default: ParticleUpdateSimple) 
        particleUpdate = new SimpleParticleUpdate(sampleParticle);

        // Set up variablesUpdate strategy (default: VariablesUpdate)
        variablesUpdate = new SimpleVariablesUpdate();

        neighborhood = null;
        neighborhoodIncrement = 0.0;
        
        constraintHandler = new NearestBoundary();
    }

    //-------------------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------------------
    /**
     * Evaluate fitness function for every particle
     * Warning: particles[] must be initialized and fitnessFunction must be set
     */
    public void evaluate() {
        if (particles == null)
            throw new RuntimeException("The swarm is not initialized");
        if (fitnessFunction == null)
            throw new RuntimeException("Fitness function is not set");

        // Initialize
        if (Double.isNaN(bestFitness)) {
            bestFitness = (fitnessFunction.isMaximize() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            bestParticleIndex = -1;
        }

        //---
        // Evaluate each particle (and find the 'best' one)
        //---
        for (int i = 0; i < particles.length; i++) {
            // Evaluate particle
            double fit = fitnessFunction.evaluate(particles[i]);

            numEvaluations++; // Update counter

            // Update 'best global' position
            if (fitnessFunction.isBetterThan(bestFitness, fit)) {
                bestFitness = fit; // Copy best fitness, index, and position vector
                bestParticleIndex = i;
                if (bestPosition == null)
                    bestPosition = new double[sampleParticle.getDimension()];
                particles[bestParticleIndex].copyPosition(bestPosition);
            }

            // Update 'best neighborhood' 
            if (neighborhood != null) {
                neighborhood.update(this, particles[i]);
            }

        }
    }

    /**
     * Make an iteration:
     * - evaluates the swarm
     * - updates positions and velocities
     * - applies positions and velocities constraints
     */
    public void evolve() {
        if (particles == null)
            init();
        evaluate(); // Evaluate particles
        update(); // Update positions and velocities
        variablesUpdate.update(this);
    }

    public double getBestFitness() {
        return bestFitness;
    }

    public Particle getBestParticle() {
        return particles[bestParticleIndex];
    }

    public int getBestParticleIndex() {
        return bestParticleIndex;
    }

    public double[] getBestPosition() {
        return bestPosition;
    }

    public FitnessFunction getFitnessFunction() {
        return fitnessFunction;
    }

    public double getGlobalIncrement() {
        return globalIncrement;
    }

    public double getInertia() {
        return inertia;
    }

    public double[] getMaxPosition() {
        return maxPosition;
    }

    public double[] getMaxVelocity() {
        return maxVelocity;
    }

    public double[] getMinPosition() {
        return minPosition;
    }

    public double[] getMinVelocity() {
        return minVelocity;
    }

    @SuppressWarnings("unchecked")
    public Neighborhood getNeighborhood() {
        return neighborhood;
    }

    /**
     * Return the best position in the neighborhood
     * Note: If neighborhood is not defined (i.e. neighborhood is null) then 'particle' is returned
     * so that it doesn't influence in particle update.
     *
     * @param particle
     * @return
     */
    @SuppressWarnings("unchecked")
    public double[] getNeighborhoodBestPosition(Particle particle) {
        if (neighborhood == null)
            return particle.getPosition();
        double d[] = neighborhood.getBestPosition(particle);
        if (d == null)
            return particle.getPosition();
        return d;
    }

    public double getNeighborhoodIncrement() {
        return neighborhoodIncrement;
    }

    public int getNumberOfEvaluations() {
        return numEvaluations;
    }

    public int getNumberOfParticles() {
        return numberOfParticles;
    }

    public Particle getParticle(int i) {
        return particles[i];
    }

    public double getParticleIncrement() {
        return particleIncrement;
    }

    public Particle[] getParticles() {
        return particles;
    }

    public ParticleUpdate getParticleUpdate() {
        return particleUpdate;
    }

    public Particle getSampleParticle() {
        return sampleParticle;
    }

    public VariablesUpdate getVariablesUpdate() {
        return variablesUpdate;
    }

    /**
     * Initialize every particle
     * Warning: maxPosition[], minPosition[], maxVelocity[], minVelocity[] must be initialized and setted
     */
    public void init() {
        if (initializer == null)
            initializer = new UniformInitialization(this);
        // Init particles
        particles = new Particle[numberOfParticles];

        // Check constraints (they will be used to initialize particles)
        if (maxPosition == null)
            throw new RuntimeException("maxPosition array is null!");
        if (minPosition == null)
            throw new RuntimeException("maxPosition array is null!");
        if (maxVelocity == null) {
            // Default maxVelocity[]
            int dim = sampleParticle.getDimension();
            maxVelocity = new double[dim];
            for (int i = 0; i < dim; i++)
                maxVelocity[i] = (maxPosition[i] - minPosition[i]) / 2.0;
        }
        if (minVelocity == null) {
            // Default minVelocity[]
            int dim = sampleParticle.getDimension();
            minVelocity = new double[dim];
            for (int i = 0; i < dim; i++)
                minVelocity[i] = -maxVelocity[i];
        }

        // Init each particle
        for (int i = 0; i < numberOfParticles; i++) {
            particles[i] = (Particle) sampleParticle.selfFactory(); // Create a new particles (using 'sampleParticle' as reference)
            particles[i].init(initializer); // Initialize it
        }

        // Init neighborhood
        if (neighborhood != null)
            neighborhood.init(this);
    }

    public void setBestParticleIndex(int bestParticle) {
        bestParticleIndex = bestParticle;
    }

    public void setBestPosition(double[] bestPosition) {
        this.bestPosition = bestPosition;
    }

    public void setFitnessFunction(FitnessFunction fitnessFunction) {
        this.fitnessFunction = fitnessFunction;
    }

    public void setGlobalIncrement(double globalIncrement) {
        this.globalIncrement = globalIncrement;
    }

    public void setInertia(double inertia) {
        this.inertia = inertia;
    }

    /**
     * Sets every maxVelocity[] and minVelocity[] to 'maxVelocity' and '-maxVelocity' respectively
     * @param maxVelocity
     */
    public void setMaxMinVelocity(double maxVelocity) {
        if (sampleParticle == null)
            throw new RuntimeException("Need to set sample particle before calling this method (use Swarm.setSampleParticle() method)");
        int dim = sampleParticle.getDimension();
        this.maxVelocity = new double[dim];
        minVelocity = new double[dim];
        for (int i = 0; i < dim; i++) {
            this.maxVelocity[i] = maxVelocity;
            minVelocity[i] = -maxVelocity;
        }
    }

    /**
     * Sets every maxPosition[] to 'maxPosition'
     * @param maxPosition
     */
    public void setMaxPosition(double maxPosition) {
        if (sampleParticle == null)
            throw new RuntimeException("Need to set sample particle before calling this method (use Swarm.setSampleParticle() method)");
        int dim = sampleParticle.getDimension();
        this.maxPosition = new double[dim];
        for (int i = 0; i < dim; i++)
            this.maxPosition[i] = maxPosition;
    }

    public void setMaxPosition(double[] maxPosition) {
        this.maxPosition = maxPosition;
    }

    public void setMaxVelocity(double[] maxVelocity) {
        this.maxVelocity = maxVelocity;
    }

    /**
     * Sets every minPosition[] to 'minPosition'
     * @param minPosition
     */
    public void setMinPosition(double minPosition) {
        if (sampleParticle == null)
            throw new RuntimeException("Need to set sample particle before calling this method (use Swarm.setSampleParticle() method)");
        int dim = sampleParticle.getDimension();
        this.minPosition = new double[dim];
        for (int i = 0; i < dim; i++)
            this.minPosition[i] = minPosition;
    }

    public void setMinPosition(double[] minPosition) {
        this.minPosition = minPosition;
    }

    public void setMinVelocity(double minVelocity[]) {
        this.minVelocity = minVelocity;
    }

    @SuppressWarnings("unchecked")
    public void setNeighborhood(Neighborhood neighborhood) {
        this.neighborhood = neighborhood;
    }

    public void setNeighborhoodIncrement(double neighborhoodIncrement) {
        this.neighborhoodIncrement = neighborhoodIncrement;
    }

    public void setNumberOfParticles(int numberOfParticles) {
        this.numberOfParticles = numberOfParticles;
    }

    public void setParticleIncrement(double particleIncrement) {
        this.particleIncrement = particleIncrement;
    }

    public void setParticles(Particle[] particle) {
        particles = particle;
    }

    public void setParticleUpdate(ParticleUpdate particleUpdate) {
        this.particleUpdate = particleUpdate;
    }

    public void setSampleParticle(Particle sampleParticle) {
        this.sampleParticle = sampleParticle;
    }

    public void setVariablesUpdate(VariablesUpdate variablesUpdate) {
        this.variablesUpdate = variablesUpdate;
    }

    /**
     * Show a swarm in a graph
     * @param graphics     : Graphics object
     * @param foreground   : foreground color
     * @param width        : graphic's width
     * @param height       : graphic's height
     * @param dim0         : Dimension to show ('x' axis)
     * @param dim1         : Dimension to show ('y' axis)
     * @param showVelocity : Show velocity tails?
     */
    public void show(Graphics graphics, Color foreground, int width, int height, int dim0, int dim1, boolean showVelocity) {
        graphics.setColor(foreground);

        if (particles != null) {
            double scalePosW = width / (maxPosition[dim0] - minPosition[dim0]);
            double scalePosH = height / (maxPosition[dim1] - minPosition[dim1]);
            double minPosW = minPosition[dim0];
            double minPosH = minPosition[dim1];

            double scaleVelW = width / (VELOCITY_GRAPH_FACTOR * (maxVelocity[dim0] - minVelocity[dim0]));
            double scaleVelH = height / (VELOCITY_GRAPH_FACTOR * (maxVelocity[dim1] - minVelocity[dim1]));
            double minVelW = minVelocity[dim0] + (maxVelocity[dim0] - minVelocity[dim0]) / 2;
            double minVelH = minVelocity[dim1] + (maxVelocity[dim1] - minVelocity[dim1]) / 2;

            for (int i = 0; i < particles.length; i++) {
                int vx, vy, x, y;
                double pos[] = particles[i].getPosition();
                double vel[] = particles[i].getVelocity();
                x = (int) (scalePosW * (pos[dim0] - minPosW));
                y = height - (int) (scalePosH * (pos[dim1] - minPosH));
                graphics.drawRect(x - 1, y - 1, 3, 3);
                if (showVelocity) {
                    vx = (int) (scaleVelW * (vel[dim0] - minVelW));
                    vy = (int) (scaleVelH * (vel[dim1] - minVelH));
                    graphics.drawLine(x, y, x + vx, y + vy);
                }
            }
        }
    }

    /** Swarm size (number of particles) */
    public int size() {
        return particles.length;
    }

    /** Printable string */
    @Override
    public String toString() {
        String str = "";

        if (particles != null)
            str += "Swarm size: " + particles.length + "\n";

        if ((minPosition != null) && (maxPosition != null)) {
            str += "Position ranges:\t";
            for (int i = 0; i < maxPosition.length; i++)
                str += "[" + minPosition[i] + ", " + maxPosition[i] + "]\t";
        }

        if ((minVelocity != null) && (maxVelocity != null)) {
            str += "\nVelocity ranges:\t";
            for (int i = 0; i < maxVelocity.length; i++)
                str += "[" + minVelocity[i] + ", " + maxVelocity[i] + "]\t";
        }

        if (sampleParticle != null)
            str += "\nSample particle: " + sampleParticle;

        if (particles != null) {
            str += "\nParticles:";
            for (int i = 0; i < particles.length; i++) {
                str += "\n\tParticle: " + i + "\t";
                str += particles[i].toString();
            }
        }
        str += "\n";

        return str;
    }

    /**
     * Return a string with some (very basic) statistics
     * @return A string
     */
    public String toStringStats() {
        String stats = "";
        if (!Double.isNaN(bestFitness)) {
            stats += "Best fitness: " + bestFitness + "\nBest position: \t[";
            for (int i = 0; i < bestPosition.length; i++)
                stats += bestPosition[i] + (i < (bestPosition.length - 1) ? ", " : "");
            stats += "]\nNumber of evaluations: " + numEvaluations + "\n";
        }
        return stats;
    }

    /**
     * Update every particle's position and velocity, also apply position and velocity constraints (if any)
     * Warning: Particles must be already evaluated
     */
    public void update() {
        // Initialize a particle update iteration
        particleUpdate.begin(this);

        // For each particle...
        for (int i = 0; i < particles.length; i++) {
            // Update particle's position and speed
            particleUpdate.update(this, particles[i]);

            // Apply position and velocity constraints
            particles[i].applyConstraints(minPosition, maxPosition, minVelocity, maxVelocity, constraintHandler);
        }

        // Finish a particle update iteration
        particleUpdate.end(this);
    }

    public ConstraintsHandler getConstraintHandler() {
        return constraintHandler;
    }

    public void setConstraintHandler(ConstraintsHandler constraintHandler) {
        this.constraintHandler = constraintHandler;
    }

    public GenericInitialization getInitializer() {
        return initializer;
    }

    public void setInitializer(GenericInitialization initializer) {
        this.initializer = initializer;
    }
    

    @Override
    public Swarm clone() throws CloneNotSupportedException {
        Swarm copy = (Swarm) super.clone();
        copy.init();
        return copy;
    }
}
