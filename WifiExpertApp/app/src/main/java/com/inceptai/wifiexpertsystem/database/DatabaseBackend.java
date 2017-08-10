package com.inceptai.wifiexpertsystem.database;

import com.inceptai.wifiexpertsystem.database.model.ActionRecord;
import com.inceptai.wifiexpertsystem.database.model.FailureRecord;
import com.inceptai.wifiexpertsystem.database.model.FeedbackRecord;
import com.inceptai.wifiexpertsystem.database.model.InferenceRecord;
import com.inceptai.wifiexpertsystem.database.model.RepairRecord;

/**
 * Created by vivek on 8/10/17.
 */

public abstract class DatabaseBackend {
    abstract public void writeActionToDatabase(final ActionRecord actionRecord);
    abstract public void writeFailureToDatabase(final FailureRecord failureRecord);
    abstract public void writeInferenceToDatabase(final InferenceRecord inferenceRecord);
    abstract public void writeFeedbackToDatabase(final FeedbackRecord feedbackRecord);
    abstract public void writeRepairToDatabase(final RepairRecord repairRecord);
}
