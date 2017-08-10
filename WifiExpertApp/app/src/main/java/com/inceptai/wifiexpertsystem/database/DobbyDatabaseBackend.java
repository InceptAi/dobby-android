package com.inceptai.wifiexpertsystem.database;

import com.inceptai.wifiexpertsystem.DobbyThreadPool;
import com.inceptai.wifiexpertsystem.database.writer.ActionDatabaseWriter;
import com.inceptai.wifiexpertsystem.database.model.ActionRecord;
import com.inceptai.wifiexpertsystem.database.writer.FailureDatabaseWriter;
import com.inceptai.wifiexpertsystem.database.model.FailureRecord;
import com.inceptai.wifiexpertsystem.database.writer.FeedbackDatabaseWriter;
import com.inceptai.wifiexpertsystem.database.model.FeedbackRecord;
import com.inceptai.wifiexpertsystem.database.writer.InferenceDatabaseWriter;
import com.inceptai.wifiexpertsystem.database.model.InferenceRecord;
import com.inceptai.wifiexpertsystem.database.writer.RepairDatabaseWriter;
import com.inceptai.wifiexpertsystem.database.model.RepairRecord;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 8/10/17.
 */
@Singleton
public class DobbyDatabaseBackend extends DatabaseBackend {
    private InferenceDatabaseWriter inferenceDatabaseWriter;
    private FailureDatabaseWriter failureDatabaseWriter;
    private RepairDatabaseWriter repairDatabaseWriter;
    private FeedbackDatabaseWriter feedbackDatabaseWriter;
    private ActionDatabaseWriter actionDatabaseWriter;

    @Inject
    public DobbyDatabaseBackend(DobbyThreadPool dobbyThreadpool) {
        inferenceDatabaseWriter = new InferenceDatabaseWriter(dobbyThreadpool);
        failureDatabaseWriter = new FailureDatabaseWriter(dobbyThreadpool);
        repairDatabaseWriter = new RepairDatabaseWriter(dobbyThreadpool);
        feedbackDatabaseWriter = new FeedbackDatabaseWriter(dobbyThreadpool);
        actionDatabaseWriter = new ActionDatabaseWriter(dobbyThreadpool);
    }

    @Override
    public void writeActionToDatabase(ActionRecord actionRecord) {
        actionDatabaseWriter.writeActionToDatabase(actionRecord);
    }

    @Override
    public void writeFailureToDatabase(FailureRecord failureRecord) {
        failureDatabaseWriter.writeFailureToDatabase(failureRecord);

    }

    @Override
    public void writeInferenceToDatabase(InferenceRecord inferenceRecord) {
        inferenceDatabaseWriter.writeInferenceToDatabase(inferenceRecord);
    }

    @Override
    public void writeFeedbackToDatabase(FeedbackRecord feedbackRecord) {
        feedbackDatabaseWriter.writeFeedbackToDatabase(feedbackRecord);
    }

    @Override
    public void writeRepairToDatabase(RepairRecord repairRecord) {
        repairDatabaseWriter.writeRepairToDatabase(repairRecord);
    }
}
