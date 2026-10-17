package com.mojang.realmsclient.client;

/**
 * Notified as a Realms download writes bytes, so the download screen can show progress.
 *
 * <p>The port's replacement for {@code java.awt.event.ActionListener}, which
 * {@code FileDownload} used together with {@code java.awt.event.ActionEvent}. Neither type
 * exists on the WASM-GC TeaVM classlib: {@code java.awt} is a desktop windowing toolkit, and
 * a browser tab has no AWT event dispatch to join. The link reported
 * {@code Class java.awt.event.ActionEvent was not found}, reached from
 * {@code FileDownload$DownloadCountingOutputStream.afterWrite}.
 *
 * <p>It is not a superset of {@code ActionListener}, and it is not trying to be. What
 * {@code ActionEvent} carried here was one field, and the two listeners read exactly one
 * thing from it:
 *
 * <pre>
 *   new ActionEvent(this, 0, null)                  // id 0, no command, source = the stream
 *   ((DownloadCountingOutputStream) event.getSource()).getByteCount()
 * </pre>
 *
 * Both the id and the null command were ignored, so the event was a one-field box whose
 * only content was "the stream that just wrote". Passing that stream as the argument says
 * the same thing without the box.
 *
 * <p>The byte count is deliberately still read from the stream rather than passed as an
 * argument. {@code CountingOutputStream.afterWrite(int)} reports how many bytes <em>this
 * write</em> moved, while both listeners want {@code getByteCount()}, the running total --
 * a different number, and using the first for the second would make the progress bar report
 * the size of the last chunk instead of the download so far.
 *
 * <p>The type is kept rather than replaced with a {@code java.util.function} interface only
 * because the argument has to be {@code FileDownload.DownloadCountingOutputStream}, a
 * package-private nested class, and naming it from a lambda-friendly interface outside
 * {@code FileDownload} would force it to be made public.
 *
 * @see FileDownload.DownloadCountingOutputStream
 * @see FileDownload.ProgressListener
 * @see FileDownload.ResourcePackProgressListener
 */
public interface DownloadProgressListener {

   /**
    * Called after every write, including zero-length ones.
    *
    * @param param0 the stream that was written to; its {@code getByteCount()} is the
    *               running total of bytes transferred so far
    */
   void onBytesWritten(FileDownload.DownloadCountingOutputStream param0);
}