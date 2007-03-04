package artofillusion.image;

import java.io.*;
import java.util.*;
import java.awt.Dimension;
import java.awt.Image;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.datasink.*;
import javax.media.format.VideoFormat;


/**
 * This program takes a list of JPEG image files and convert them into
 * a QuickTime movie.
 */
public class MovieEncoder implements ControllerListener, DataSinkListener {
  
  public Processor p;
  public DataSink dsink;
  public ImageDataSource ids;
  
  
  public void pushBytes(byte[] b) {
    ids.streams[0].putImageToCache(b);
  
  }
  
  
  public void signalDone() {
    ids.streams[0].signalDone();
    waitForFileDone();
    cleanUp();
  
  }
  
  
  public MovieEncoder(int width, int height, int frameRate, File file) {
    
    try {
      MediaLocator outML = new MediaLocator(file.toURL());    
      
      ids = new ImageDataSource(width, height, frameRate);
      System.err.println("- create processor for the image datasource ..." + file.getName());
      p = Manager.createProcessor(ids);
      
      p.addControllerListener(this);
      
      // Put the Processor into configured state so we can set
      // some processing options on the processor.
      p.configure();
      if (!waitForState(p, p.Configured)) {
      
      }
      
      // Set the output content descriptor to QuickTime. 
      p.setContentDescriptor(new ContentDescriptor(FileTypeDescriptor.QUICKTIME));
      
      // Query for the processor for supported formats.
      // Then set it on the processor.
      TrackControl tcs[] = p.getTrackControls();
      Format f[] = tcs[0].getSupportedFormats();
      if (f == null || f.length <= 0) {
        throw new RuntimeException("The mux does not support the input format: " + tcs[0].getFormat());
      }
      
      tcs[0].setFormat(f[0]);
      
      System.err.println("Setting the track format to: " + f[0]);
      
      // We are done with programming the processor.  Let's just
      // realize it.
      p.realize();
      if (!waitForState(p, p.Realized)) {
        
        throw new RuntimeException(("Failed to realize the processor."));
      }
      
      // Now, we'll need to create a DataSink.
      if ((dsink = createDataSink(p, outML)) == null) {
        
        throw new RuntimeException(("Failed to create a DataSink for the given output MediaLocator: " + outML));
      }
      
      dsink.addDataSinkListener(this);
    
    } catch (Exception e) {
      e.printStackTrace();  
    }
  
  } // end constructor.
  
  public boolean doIt() {
    
    // OK, we can now start the actual transcoding.
    try {
      p.start();
      dsink.start();
    } catch (IOException e) {
      System.err.println("IO error during processing");
      return false;
    }
    
    // Wait for EndOfStream event.
    // waitForFileDone();
    
    return true;  
  }
  
  
  public void cleanUp() {
    
    System.err.println("Closing DataSink...");
    
    try {
      dsink.close();
    } catch (Exception e) {}
    p.removeControllerListener(this);
  
  }
  
  
    /**
     * Create the DataSink.
     */
  
  DataSink createDataSink(Processor p, MediaLocator outML) {
    
    DataSource ds;
    
    if ((ds = p.getDataOutput()) == null) {
      System.err.println("Something is really wrong: the processor does not have an output DataSource");
      return null;
    }
    
    DataSink dsink;
    
    try {
      System.err.println("- create DataSink for: " + outML.getURL() + " " + outML.getProtocol());
      dsink = Manager.createDataSink(ds, outML);
      dsink.open();
    } catch (Exception e) {
      System.err.println("Cannot create the DataSink: ");
      e.printStackTrace();
      return null;
    }
    
    return dsink;
  }
  
  
  Object waitSync = new Object();
  boolean stateTransitionOK = true;
  
    /**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     */
  boolean waitForState(Processor p, int state) {
    synchronized (waitSync) {
      try {
        while (p.getState() < state && stateTransitionOK)
          waitSync.wait();
      } catch (Exception e) {}
    }
    return stateTransitionOK;
  }
  
  
    /**
     * Controller Listener.
     */
  public void controllerUpdate(ControllerEvent evt) {
    
    if (evt instanceof ConfigureCompleteEvent ||
      evt instanceof RealizeCompleteEvent ||
      evt instanceof PrefetchCompleteEvent) {
      synchronized (waitSync) {
        stateTransitionOK = true;
        waitSync.notifyAll();
      }
    } else if (evt instanceof ResourceUnavailableEvent) {
      synchronized (waitSync) {
        stateTransitionOK = false;
        waitSync.notifyAll();
      }
    } else if (evt instanceof EndOfMediaEvent) {
      evt.getSourceController().stop();
      evt.getSourceController().close();
    }
  }
  
  
  Object waitFileSync = new Object();
  boolean fileDone = false;
  boolean fileSuccess = true;
  
    /**
     * Block until file writing is done. 
     */
  boolean waitForFileDone() {
    synchronized (waitFileSync) {
      try {
        while (!fileDone)
          waitFileSync.wait();
      } catch (Exception e) {}
    }
    return fileSuccess;
  }
  
  
    /**
     * Event handler for the file writer.
     */
  public void dataSinkUpdate(DataSinkEvent evt) {
    
    if (evt instanceof EndOfStreamEvent) {
      synchronized (waitFileSync) {
        fileDone = true;
        waitFileSync.notifyAll();
      }
    } else if (evt instanceof DataSinkErrorEvent) {
      synchronized (waitFileSync) {
        fileDone = true;
        fileSuccess = false;
        waitFileSync.notifyAll();
      }
    }
  }
  
  
  ///////////////////////////////////////////////
  //
  // Inner classes.
  ///////////////////////////////////////////////
  
  
    /**
     * A DataSource to read from a list of JPEG image files and
     * turn that into a stream of JMF buffers.
     * The DataSource is not seekable or positionable.
     */
  public class ImageDataSource extends PullBufferDataSource {
    
    protected ImageSourceStream streams[];
    
    ImageDataSource(int width, int height, int frameRate) {
      streams = new ImageSourceStream[1];
      streams[0] = new ImageSourceStream(width, height, frameRate);
    }
    
    public void setLocator(MediaLocator source) {
    }
    
    
    public MediaLocator getLocator() {
      return null;
    }
    
  /**
   * Content type is of RAW since we are sending buffers of video
   * frames without a container format.
   */
    public String getContentType() {
      return ContentDescriptor.RAW;
    }
    
    public void connect() {
    }
    
    public void disconnect() {
    }
    
    public void start() {
    }
    
    public void stop() {
    
    }
    
  /**
   * Return the ImageSourceStreams.
   */
    public PullBufferStream[] getStreams() {
      return streams;
    }
    
  /**
   * We could have derived the duration from the number of
   * frames and frame rate.  But for the purpose of this program,
   * it's not necessary.
   */
    public Time getDuration() {
      return DURATION_UNKNOWN;
    }
    
    public Object[] getControls() {
      return new Object[0];
    }
    
    public Object getControl(String type) {
      return null;
    }
  }
  
  
    /**
     * The source stream to go along with ImageDataSource.
     */
  class ImageSourceStream implements PullBufferStream {
    
    Vector cache = new Vector();
    int width, height;
    VideoFormat format;
    Object waitLock = new Object();
    
    int nextImage = 0;  // index of the next image to be read.
    boolean ended = false;
    boolean blocking = false;
    
    public ImageSourceStream(int width, int height, int frameRate) {
      
      this.width = width;
      this.height = height;
      
      format = new VideoFormat(VideoFormat.JPEG,
        new Dimension(width, height),
        Format.NOT_SPECIFIED,
        Format.byteArray,
        (float)frameRate);
    }
    
  /**
   * We block when the cache is empty.
   */
    
    public boolean willReadBlock() {
      return blocking;
    }
    
    
    public void signalDone() {
      
      ended = true;
      synchronized (waitLock) {
        waitLock.notifyAll();
      }
    
    }
    
    public void putImageToCache(byte[] image) {
      
      boolean wasEmpty = cache.isEmpty();
      
      cache.add(image); // Vector is synchronized.
      
      if (wasEmpty) {
        synchronized (waitLock) {
          waitLock.notify();
        }  
      }
    }
    
    
    private byte[] getImageFromCache() {
      
      if (cache.isEmpty() && !ended) {
        synchronized(waitLock) {
          
          blocking = true;
          
          try {
            waitLock.wait();
            
            if (ended || cache.isEmpty()) {
              return null; 
            }
          
          } catch (InterruptedException rupt) {
            rupt.printStackTrace(); // not a common occurrence.
          }
          
        }
      blocking = false;
      
      } else if (ended) { return null; } 
      
      return (byte[])cache.remove(0); // Vector is synchronized.
    }
    
  /**
   * This is called from the Processor to read a frame worth
   * of video data.
   */
    public void read(Buffer buf) throws IOException {
      
      byte[] image = getImageFromCache();
      
      if (image == null) {
        
        System.err.println("Done reading all images.");
        buf.setEOM(true);
        buf.setOffset(0);
        buf.setLength(0);
        // ended = true; // must be set by client.
        return;
      }
      
      // Check the input buffer type & size.
      
      /* if (buf.getData() instanceof byte[])
        data = (byte[])buf.getData(); // for future optimisation. */ 
      
      buf.setData(image);
      
      /* Check to see the given buffer is big enough for the frame.
      if (data == null || data.length < image.length) {
    data = new byte[(int)image.length];
    buf.setData(data); // from Sun; redundant?
      } */
      
      
      buf.setOffset(0);
      buf.setLength(image.length);
      buf.setFormat(format);
      buf.setFlags(buf.getFlags() | buf.FLAG_KEY_FRAME);
    
    }
    
  /**
   * Return the format of each video frame.  That will be JPEG.
   */
    public Format getFormat() {
      return format;
    }
    
    public ContentDescriptor getContentDescriptor() {
      return new ContentDescriptor(ContentDescriptor.RAW);
    }
    
    public long getContentLength() {
      return 0;
    }
    
    public boolean endOfStream() {
      return ended;
    }
    
    public Object[] getControls() {
      return new Object[0];
    }
    
    public Object getControl(String type) {
      return null;
    }
  }
}
