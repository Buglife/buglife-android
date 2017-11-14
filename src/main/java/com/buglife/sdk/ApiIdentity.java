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

import android.os.Parcel;
import android.os.Parcelable;

interface ApiIdentity extends Parcelable {
    String getId();

    class ApiKey implements ApiIdentity {
        private final String mApiKey;

        ApiKey(String apiKey) {
            this.mApiKey = apiKey;
        }

        @Override public String getId() {
            return mApiKey;
        }

        /* Parcelable */

        @Override public int describeContents() {
            return 0;
        }

        @Override public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mApiKey);
        }

        ApiKey(Parcel in) {
            mApiKey = in.readString();
        }

        public static final Creator<ApiKey> CREATOR = new Creator<ApiKey>() {
            @Override public ApiKey createFromParcel(Parcel source) {
                return new ApiKey(source);
            }

            @Override public ApiKey[] newArray(int size) {
                return new ApiKey[size];
            }
        };
    }

    class EmailAddress implements ApiIdentity {
        private final String mEmailAddress;

        EmailAddress(String emailAddress) {
            this.mEmailAddress = emailAddress;
        }

        @Override public String getId() {
            return mEmailAddress;
        }

        /* Parcelable */

        @Override public int describeContents() {
            return 0;
        }

        @Override public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mEmailAddress);
        }

        EmailAddress(Parcel in) {
            mEmailAddress = in.readString();
        }

        public static final Creator<EmailAddress> CREATOR = new Creator<EmailAddress>() {
            @Override public EmailAddress createFromParcel(Parcel source) {
                return new EmailAddress(source);
            }

            @Override public EmailAddress[] newArray(int size) {
                return new EmailAddress[size];
            }
        };
    }
}