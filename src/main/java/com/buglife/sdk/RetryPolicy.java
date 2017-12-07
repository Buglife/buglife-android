/*
 * Copyright (C) 2017 Buglife, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.buglife.sdk;

/**
 * The retry policy for submitting bug reports.
 * @warning This is an experimental API, and is subject to change!
 */
public enum RetryPolicy {
    /**
     * Specifies that bug report submission should occur asynchronously.
     * This will be using SubmitReportService when running Android Lollipop, and a legacy
     * service otherwise. Failed bug reports are reattempted when the SubmitReportService permits.
     */
    AUTOMATIC,

    /**
     * Forces use of the legacy service for report submission. This should generally be used
     * for testing only.
     */
    AUTOMATIC_LEGACY,

    /**
     * Specifies that bug report submission should be a blocking operation;
     * A loading indicator will be presented over the bug reporter UI until
     * bug report submission is complete, or fails. If bug report submission fails,
     * then the user will be able to manually retry submitting the bug report.
     */
    MANUAL
}
