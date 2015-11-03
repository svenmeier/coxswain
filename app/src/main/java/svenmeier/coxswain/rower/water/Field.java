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
package svenmeier.coxswain.rower.water;

import svenmeier.coxswain.gym.Snapshot;

/**
 */
public abstract class Field {

    public String request;

    public String response;

    protected Field() {
    }

    protected Field(String request, String response) {
        this.request = request;
        this.response = response;
    }

    protected void update(String message, Snapshot memory) {
        if (message.startsWith(response)) {
            onUpdate(message, memory);
        }
    }

    protected abstract void onUpdate(String message, Snapshot memory);
}
