/*
 *
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * 
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 * 
 * You may elect to redistribute this code under either of these licenses.
 */

package de.braintags.io.vertx.pojomapper.mongo.test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.braintags.io.vertx.pojomapper.annotation.field.Id;
import de.braintags.io.vertx.pojomapper.exception.MappingException;
import de.braintags.io.vertx.pojomapper.mapping.IField;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.mongo.test.mapper.Person;

/**
 * 
 *
 * @author Michael Remme
 * 
 */

public class TestMongoMapper extends MongoBaseTest {
  private static Logger logger = LoggerFactory.getLogger(TestMongoMapper.class);
  private static IMapper mapperDef = null;

  @BeforeClass
  public static void beforeClass() throws Exception {
    System.setProperty("connection_string", "mongodb://localhost:27017");
    System.setProperty("db_name", "PojongoTestDatabase");
    MongoBaseTest.startMongo();
  }

  @AfterClass
  public static void afterClass() {
    MongoBaseTest.stopMongo();
  }

  IMapper getMapper() {
    if (mapperDef == null)
      mapperDef = getDataStore().getMapperFactory().getMapper(Person.class);
    return mapperDef;
  }

  /**
   * 
   */
  public TestMongoMapper() {
  }

  @Test
  public void testId() {
    IMapper mapperDef = getMapper();
    IField idField = mapperDef.getField("idField");
    Assert.assertNotNull("Improve that the name of the id field is 'idField'", idField);

    Id ann = (Id) mapperDef.getField("idField").getAnnotation(Id.class);
    if (ann == null)
      Assert.fail("Annotation Id must not be null");

    IField field = mapperDef.getIdField();
    assertNotNull(field);
    IField field2 = mapperDef.getField(field.getName());
    Assert.assertSame(field, field2);

    String javaName = field.getName();
    String dbName = field.getMappedFieldName();
    Assert.assertNotEquals(javaName, dbName);

    try {
      field = mapperDef.getField(field.getMappedFieldName());
      fail("this should throw an exception here");
    } catch (MappingException e) {
      // this is the expected result
    }

  }

}