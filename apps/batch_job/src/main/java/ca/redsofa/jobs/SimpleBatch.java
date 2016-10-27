package ca.redsofa.jobs;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.Encoders;
import ca.redsofa.domain.Person;
import ca.redsofa.udf.StringLengthUdf;
import static org.apache.spark.sql.functions.callUDF;

public class SimpleBatch 
{

	private static String INPUT_FILE = "people.json";

	public static void main(String[] args) {
		SimpleBatch job = new SimpleBatch();
		job.startJob();
	}
	  
	private void startJob( ){
	    System.out.println("Stating Job...");

	    long startTime = System.currentTimeMillis();	    
		SparkSession spark = SparkSession
				.builder()
				.appName("Simple Batch Job")
				.config("spark.eventLog.enabled", "false")
				.config("spark.driver.memory", "2g")
				.config("spark.executor.memory", "2g")
				.enableHiveSupport()
				.getOrCreate();


		Dataset<Person> sourceDS = spark.read().json(INPUT_FILE).as(Encoders.bean(Person.class));
		sourceDS.createOrReplaceTempView("people");
		sourceDS.sample(false, .50, 12345).show();

		//Adding a calculated column with UDF 
		StringLengthUdf.registerStringLengthUdf(spark);		
		//Creates a new Datasource based on the sourceDS
		Dataset<Row> newDS = sourceDS.withColumn("name_length", callUDF("stringLengthUdf", sourceDS.col("firstName")));    
		newDS.sample(false, .50, 12345).show();


		Dataset<Row> kids = spark.sql("SELECT * FROM people WHERE age <= 12");
		kids.sample(false, .50, 12345).show();


		Dataset<Row> ageAverage = spark.sql("SELECT AVG(age) as average_age, sex FROM people GROUP BY sex");
		ageAverage.sample(false, .50, 12345).show();

		spark.stop();
		
	    long stopTime = System.currentTimeMillis();
	    long elapsedTime = stopTime - startTime;
	    System.out.println("Execution time in ms : " + elapsedTime);
	}
}
