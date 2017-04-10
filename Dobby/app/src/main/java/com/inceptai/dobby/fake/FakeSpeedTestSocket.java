package com.inceptai.dobby.fake;

import com.inceptai.dobby.speedtest.SpeedTestConfig;

import java.math.RoundingMode;

import fr.bmartel.speedtest.RepeatWrapper;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestMode;
import fr.bmartel.speedtest.model.UploadStorageType;

/**
 * FakeSpeedTestSocket allows fake behavior to be programmed.
 * Other related fake classes: SpeedTestReport and SpeedTestError.
 */

public class FakeSpeedTestSocket extends SpeedTestSocket {
    public FakeSpeedTestSocket() {
        super();
    }

    @Override
    public void addSpeedTestListener(ISpeedTestListener listener) {
        // IMPLEMENT.
        // super.addSpeedTestListener(listener);
    }

    @Override
    public void removeSpeedTestListener(ISpeedTestListener listener) {
        super.removeSpeedTestListener(listener);
    }

    @Override
    public void shutdownAndWait() {
        super.shutdownAndWait();
    }

    @Override
    public void startFixedDownload(String hostname, int port, String uri, int maxDuration) {
        super.startFixedDownload(hostname, port, uri, maxDuration);
    }

    @Override
    public void startFixedDownload(String hostname, int port, String uri, int maxDuration, int reportInterval) {
        super.startFixedDownload(hostname, port, uri, maxDuration, reportInterval);
    }

    @Override
    public void startDownload(String hostname, String uri) {
        super.startDownload(hostname, uri);
    }

    @Override
    public void startDownload(String hostname, String uri, int reportInterval) {
        super.startDownload(hostname, uri, reportInterval);
    }

    @Override
    public void startDownload(String hostname, int port, String uri, int reportInterval) {
        super.startDownload(hostname, port, uri, reportInterval);
    }

    @Override
    public void startDownload(String hostname, int port, String uri) {
        super.startDownload(hostname, port, uri);
    }

    @Override
    public void startFtpDownload(String hostname, String uri, int reportInterval) {
        super.startFtpDownload(hostname, uri, reportInterval);
    }

    @Override
    public void startFtpFixedDownload(String hostname, String uri, int maxDuration, int reportInterval) {
        super.startFtpFixedDownload(hostname, uri, maxDuration, reportInterval);
    }

    @Override
    public void startFtpFixedDownload(String hostname, String uri, int maxDuration) {
        super.startFtpFixedDownload(hostname, uri, maxDuration);
    }

    @Override
    public void startFtpDownload(String hostname, String uri) {
        super.startFtpDownload(hostname, uri);
    }

    @Override
    public void startFtpDownload(String hostname, int port, String uri, String user, String password) {
        super.startFtpDownload(hostname, port, uri, user, password);
    }

    @Override
    public void startFixedUpload(String hostname, int port, String uri, int fileSizeOctet, int maxDuration) {
        super.startFixedUpload(hostname, port, uri, fileSizeOctet, maxDuration);
    }

    @Override
    public void startFixedUpload(String hostname, int port, String uri, int fileSizeOctet, int maxDuration, int reportInterval) {
        super.startFixedUpload(hostname, port, uri, fileSizeOctet, maxDuration, reportInterval);
    }

    @Override
    public void startFtpFixedUpload(String hostname, String uri, int fileSizeOctet, int maxDuration, int reportInterval) {
        super.startFtpFixedUpload(hostname, uri, fileSizeOctet, maxDuration, reportInterval);
    }

    @Override
    public void startFtpFixedUpload(String hostname, String uri, int fileSizeOctet, int maxDuration) {
        super.startFtpFixedUpload(hostname, uri, fileSizeOctet, maxDuration);
    }

    @Override
    public void startFtpUpload(String hostname, String uri, int fileSizeOctet, int reportInterval) {
        super.startFtpUpload(hostname, uri, fileSizeOctet, reportInterval);
    }

    @Override
    public void startFtpUpload(String hostname, String uri, int fileSizeOctet) {
        super.startFtpUpload(hostname, uri, fileSizeOctet);
    }

    @Override
    public void startFtpUpload(String hostname, int port, String uri, int fileSizeOctet, String user, String password) {
        super.startFtpUpload(hostname, port, uri, fileSizeOctet, user, password);
    }

    @Override
    public void startUpload(String hostname, String uri, int fileSizeOctet) {
        super.startUpload(hostname, uri, fileSizeOctet);
    }

    @Override
    public void startUpload(String hostname, String uri, int fileSizeOctet, int reportInterval) {
        super.startUpload(hostname, uri, fileSizeOctet, reportInterval);
    }

    @Override
    public void startUpload(String hostname, int port, String uri, int fileSizeOctet, int reportInterval) {
        super.startUpload(hostname, port, uri, fileSizeOctet, reportInterval);
    }

    @Override
    public void startUpload(String hostname, int port, String uri, int fileSizeOctet) {
        super.startUpload(hostname, port, uri, fileSizeOctet);
    }

    @Override
    public void startDownloadRepeat(String hostname, String uri, int repeatWindow, int reportPeriodMillis, IRepeatListener repeatListener) {
        super.startDownloadRepeat(hostname, uri, repeatWindow, reportPeriodMillis, repeatListener);
    }

    @Override
    public void startDownloadRepeat(String hostname, int port, String uri, int repeatWindow, int reportPeriodMillis, IRepeatListener repeatListener) {
        super.startDownloadRepeat(hostname, port, uri, repeatWindow, reportPeriodMillis, repeatListener);
    }

    @Override
    public void startUploadRepeat(String hostname, String uri, int repeatWindow, int reportPeriodMillis, int fileSizeOctet, IRepeatListener repeatListener) {
        super.startUploadRepeat(hostname, uri, repeatWindow, reportPeriodMillis, fileSizeOctet, repeatListener);
    }

    @Override
    public void startUploadRepeat(String hostname, int port, String uri, int repeatWindow, int reportPeriodMillis, int fileSizeOctet, IRepeatListener repeatListener) {
        super.startUploadRepeat(hostname, port, uri, repeatWindow, reportPeriodMillis, fileSizeOctet, repeatListener);
    }

    @Override
    public void forceStopTask() {
        // IMPLEMENT.
        // super.forceStopTask();
    }

    @Override
    public SpeedTestReport getLiveDownloadReport() {
        return super.getLiveDownloadReport();
    }

    @Override
    public SpeedTestReport getLiveUploadReport() {
        return super.getLiveUploadReport();
    }

    @Override
    public void closeSocket() {
        super.closeSocket();
    }

    @Override
    public SpeedTestMode getSpeedTestMode() {
        return super.getSpeedTestMode();
    }

    @Override
    public void setSocketTimeout(int socketTimeoutMillis) {
        super.setSocketTimeout(socketTimeoutMillis);
    }

    @Override
    public int getSocketTimeout() {
        return super.getSocketTimeout();
    }

    @Override
    public int getUploadChunkSize() {
        return super.getUploadChunkSize();
    }

    @Override
    public RepeatWrapper getRepeatWrapper() {
        return super.getRepeatWrapper();
    }

    @Override
    public void setUploadChunkSize(int uploadChunkSize) {
        super.setUploadChunkSize(uploadChunkSize);
    }

    @Override
    public void setDefaultRoundingMode(RoundingMode roundingMode) {
        super.setDefaultRoundingMode(roundingMode);
    }

    @Override
    public void setDefaultScale(int scale) {
        super.setDefaultScale(scale);
    }

    @Override
    public void setUploadSetupTime(long setupTime) {
        super.setUploadSetupTime(setupTime);
    }

    @Override
    public void setDownloadSetupTime(long setupTime) {
        super.setDownloadSetupTime(setupTime);
    }

    @Override
    public long getDownloadSetupTime() {
        return super.getDownloadSetupTime();
    }

    @Override
    public long getUploadSetupTime() {
        return super.getUploadSetupTime();
    }

    @Override
    public RoundingMode getDefaultRoundingMode() {
        return super.getDefaultRoundingMode();
    }

    @Override
    public int getDefaultScale() {
        return super.getDefaultScale();
    }

    @Override
    public UploadStorageType getUploadStorageType() {
        throw new RuntimeException("Unimplemented.");
        // return super.getUploadStorageType();
    }

    @Override
    public void setUploadStorageType(UploadStorageType uploadStorageType) {
        throw new RuntimeException("Unimplemented.");
        // super.setUploadStorageType(uploadStorageType);
    }

    @Override
    public void clearListeners() {
        super.clearListeners();
    }
}
