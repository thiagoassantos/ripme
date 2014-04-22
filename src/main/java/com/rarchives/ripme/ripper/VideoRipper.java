package com.rarchives.ripme.ripper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.rarchives.ripme.ui.RipStatusMessage;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;

public abstract class VideoRipper extends AbstractRipper {

    private int bytesTotal = 1,
                 bytesCompleted = 1;

    public VideoRipper(URL url) throws IOException {
        super(url);
    }

    public abstract boolean canRip(URL url);
    public abstract void rip() throws IOException;
    public abstract String getHost();
    public abstract String getGID(URL url) throws MalformedURLException;

    @Override
    public void setBytesTotal(int bytes) {
        this.bytesTotal = bytes;
    }
    @Override
    public void setBytesCompleted(int bytes) {
        this.bytesCompleted = bytes;
    }

    @Override
    public String getAlbumTitle(URL url) {
        return "videos";
    }

    @Override
    public void addURLToDownload(URL url, File saveAs) {
        threadPool.addThread(new DownloadVideoThread(url, saveAs, this));
    }

    @Override
    public void setWorkingDir(URL url) throws IOException {
        String path = Utils.getWorkingDirectory().getCanonicalPath();
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        path += "videos" + File.separator;
        this.workingDir = new File(path);
        if (!this.workingDir.exists()) {
            logger.info("[+] Creating directory: " + Utils.removeCWD(this.workingDir));
            this.workingDir.mkdirs();
        }
        logger.debug("Set working directory to: " + this.workingDir);
    }

    @Override
    public int getCompletionPercentage() {
        return (int) (100 * (bytesCompleted / (float) bytesTotal));
    }

    @Override
    public void downloadCompleted(URL url, File saveAs) {
        if (observer == null) {
            return;
        }
        try {
            String path = Utils.removeCWD(saveAs);
            RipStatusMessage msg = new RipStatusMessage(STATUS.DOWNLOAD_COMPLETE, path);
            observer.update(this, msg);

            checkIfComplete();
        } catch (Exception e) {
            logger.error("Exception while updating observer: ", e);
        }
    }
    @Override
    public void downloadErrored(URL url, String reason) {
        if (observer == null) {
            return;
        }
        observer.update(this, new RipStatusMessage(STATUS.DOWNLOAD_ERRORED, url + " : " + reason));
        checkIfComplete();
    }
    @Override
    public void downloadProblem(URL url, String message) {
        if (observer == null) {
            return;
        }
        observer.update(this, new RipStatusMessage(STATUS.DOWNLOAD_WARN, url + " : " + message));
        checkIfComplete();
    }

    @Override
    public String getStatusText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCompletionPercentage())
          .append("% ")
          .append(" - ")
          .append(Utils.bytesToHumanReadable(bytesCompleted))
          .append(" / ")
          .append(Utils.bytesToHumanReadable(bytesTotal));
        return sb.toString();
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    /**
     * Notifies observers and updates state if all files have been ripped.
     */
    @Override
    protected void checkIfComplete() {
        if (observer == null) {
            return;
        }
        if (bytesCompleted >= bytesTotal) {
            super.checkIfComplete();
        }
    }
}