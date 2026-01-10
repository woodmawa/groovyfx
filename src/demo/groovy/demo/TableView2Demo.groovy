/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2011-2021 the original author or authors.
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
package demo

import groovy.transform.Canonical
import groovyx.javafx.beans.FXBindable

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static groovyx.javafx.GroovyFX.start

enum TableGender {
    MALE, FEMALE
}

@Canonical
class TablePerson {
    @FXBindable String name
    @FXBindable int age
    @FXBindable TableGender gender
    @FXBindable LocalDate dob
}

def persons = [
        new TablePerson(name: "Jim Clarke",     age: 29, gender: TableGender.MALE,   dob: LocalDate.now().minusDays(90)),
        new TablePerson(name: "Dean Iverson",   age: 30, gender: TableGender.MALE,   dob: LocalDate.now().minusDays(45)),
        new TablePerson(name: "Angelina Jolie", age: 36, gender: TableGender.FEMALE, dob: LocalDate.now())
]

def dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy")

start {
    stage(title: "GroovyFX Table Demo", width: 500, height: 200, visible: true) {
        scene(fill: GROOVYBLUE) {
            tableView(selectionMode: "single", cellSelectionEnabled: true, editable: true, items: persons) {

                tableColumn(editable: true, property: "name", text: "Name", prefWidth: 150,
                        onEditCommit: { event ->
                            TablePerson item = event.tableView.items.get(event.tablePosition.row)
                            item.name = event.newValue
                        }
                )

                tableColumn(editable: true, property: "age", text: "Age", prefWidth: 50, type: Integer,
                        onEditCommit: { event ->
                            TablePerson item = event.tableView.items.get(event.tablePosition.row)
                            item.age = Integer.valueOf(event.newValue)
                        }
                )

                tableColumn(editable: true, property: "gender", text: "Gender", prefWidth: 150, type: TableGender,
                        onEditCommit: { event ->
                            TablePerson item = event.tableView.items.get(event.tablePosition.row)
                            item.gender = event.newValue
                        }
                )

                tableColumn(editable: true, property: "dob", text: "Birth", prefWidth: 150, type: LocalDate,
                        converter: { from -> dateFormat.format(from) },
                        onEditCommit: { event ->
                            TablePerson item = event.tableView.items.get(event.tablePosition.row)
                            item.dob = LocalDate.parse(event.newValue, dateFormat)
                        }
                )
            }
        }
    }
}
