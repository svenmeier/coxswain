/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain.rower.wired;

import svenmeier.coxswain.gym.Measurement;

/**
 */
public class Field {

    public String request;

    public String response;

    protected Field() {
    }

    protected Field(String request, String response) {
        this.request = request;
        this.response = response;
    }

    protected boolean input(String message, Measurement measurement) {
        if (this.response != null && message.startsWith(response)) {
            onInput(message, measurement);

            return true;
        }

        return false;
    }

    protected void onInput(String message, Measurement measurement) {

    }

    protected void onAfterOutput() {
    }
}
