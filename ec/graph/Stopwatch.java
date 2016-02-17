/*=====================================================================
 *                      PlanWare Java Utilities
 *              Copyright (c) 2005, 2008 Kenneth B. Kopelson
 * 
 * Author  : Ken Kopelson
 * Created : Jun 30, 2005
 * File    : Stopwatch.java
 *
 *=====================================================================*/
package ec.graph;

public class Stopwatch {
    public  static boolean useNanoTime  = false;
    private long    startTime    = -1L;
    private long    stopTime     = -1L;
    private long    restartTime  = -1L;
    private long    totalPaused  = 0L;
    private long    intervalTime = -1L;
    private boolean running   = false;

    public Stopwatch start() {
        startTime   = intervalTime = useNanoTime ? System.nanoTime() : System.currentTimeMillis();
        stopTime    = -1L;
        restartTime = -1L;
        totalPaused  = 0L;
        running     = true;
        return this;
    }
    
    public Stopwatch stop() {
        if ( running ) { 
            stopTime = useNanoTime ? System.nanoTime() : System.currentTimeMillis();
            running  = false;
        }
        return this;
    }
    
    public Stopwatch restart() {
        if ( stopTime != -1L ) {
            restartTime = intervalTime = useNanoTime ? System.nanoTime() : System.currentTimeMillis();
            totalPaused += restartTime - stopTime;
            stopTime = -1L;
            running  = true;
        }
        return this;
    }
    
    public long getElapsedTime() {
        if ( startTime == -1L )
            return 0;
        if ( running )
            return ( useNanoTime ? System.nanoTime() : System.currentTimeMillis() ) - startTime - totalPaused;
        else
            return stopTime - startTime - totalPaused;
    }
    
    public long getLastInterval() {
        if ( startTime == -1L )
            return 0;
        if ( running ) {
            long curTime  = useNanoTime ? System.nanoTime() : System.currentTimeMillis();
            long interval = curTime - intervalTime;
            intervalTime  = curTime;
            return interval;
        }
        else
            return stopTime - intervalTime;
    }
    
    public Stopwatch reset() {
        startTime    = -1L;
        intervalTime = -1L;
        stopTime     = -1L;
        restartTime  = -1L;
        totalPaused   = 0L;
        if ( running )
            start();
        return this;
    }
    public long getStartTime()   { return startTime; }
    public long getStopTime()    { return stopTime; }
    public long getTotalPaused() { return totalPaused; }
    public boolean isRunning()   { return running; }
    
    public String toString( String msg ) {
        long          interval = getLastInterval();
        long          paused   = getTotalPaused();
        StringBuilder sb       = new StringBuilder();
        String unitStr = useNanoTime ? "ns" : "ms";
        sb.append( "Elapsed: " ).append( getElapsedTime() ).append( unitStr );
        sb.append( " ( Interval: " ).append( interval ).append( unitStr );
        if ( paused > 0L )
            sb.append( ", Paused: " ).append( getTotalPaused() ).append( unitStr );
        sb.append( " )" );
        if ( msg != null )
            sb.append( ": " ).append( msg );
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString( null );
    }
    public void printMessage( String msg ) {
        System.out.println( toString( msg ));
    }
    public void printMessage() {
        printMessage( null );
    }
}
