package ch.mrieser.serializationtest;

import ch.mrieser.serializationtest.pbf.PopulationPBF;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.michaz.matsimavro.AvroPersonWriter;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author mrieser
 */
public class SerializationTest {

	private final static Logger log = Logger.getLogger(SerializationTest.class);

	public static void main(String[] args) throws IOException {
		String populationFilename = "/data/vis/santiago/v1/santiago/output/baseCase/output_plans.xml.gz";


		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		Population population = scenario.getPopulation();
		new PopulationReader(scenario).readFile(populationFilename);

		for (int loop = 1; loop <= 5; loop++) {
			log.info("LOOP " + loop);

			String baseName = "population_" + loop;

			log.info("[" + loop + "]  xml");
			long start1 = System.currentTimeMillis();
			new PopulationWriter(population).write(baseName + ".xml");
			long end1 = System.currentTimeMillis();

			log.info("[" + loop + "]  xml.gz");
			long start2 = System.currentTimeMillis();
			new PopulationWriter(population).write(baseName + ".xml.gz");
			long end2 = System.currentTimeMillis();

			log.info("[" + loop + "]  kryo");
			long start3 = System.currentTimeMillis();
			Kryo kryo3 = new Kryo();
			Output output3 = new Output(new FileOutputStream(baseName + ".kryo"));
			kryo3.writeObject(output3, population);
			output3.close();
			long end3 = System.currentTimeMillis();

			log.info("[" + loop + "]  kryo.lz4");
			long start4 = System.currentTimeMillis();
			Kryo kryo4 = new Kryo();
			Output output4 = new Output(new LZ4BlockOutputStream(new FileOutputStream(baseName + ".kryo.lz4")));
			kryo4.writeObject(output4, population);
			output4.close();
			long end4 = System.currentTimeMillis();

//			log.info("[" + loop + "]  kryo.custom");
//			long start5 = System.currentTimeMillis();
//			Kryo kryo5 = new Kryo();
//			kryo5.register(Person.class, new PersonSerializer()); // doesn't work, as we would have to register PersonImpl.class, but that's not visible…
//			Output output5 = new Output(new FileOutputStream(baseName + ".kryo.custom"));
//
//			kryo5.writeObject(output5, population);
//			output5.close();
//			long end5 = System.currentTimeMillis();

//			log.info("[" + loop + "]  fst");
//			long start6 = System.currentTimeMillis();
//			{ // does not work, as it's not Serializable
//				FSTObjectOutput out = new FSTObjectOutput(new FileOutputStream(baseName + ".fst"));
//				out.writeObject(population);
//				out.close(); // required !
//			}
//			long end6 = System.currentTimeMillis();

			log.info("[" + loop + "]  pbf");
			long start7 = System.currentTimeMillis();
			OutputStream output7 = new BufferedOutputStream(new FileOutputStream(baseName + ".pbf"));
			writePbf(population, output7);
			output7.close();
			long end7 = System.currentTimeMillis();

			log.info("[" + loop + "]  pbf.lz4");
			long start8 = System.currentTimeMillis();
			OutputStream output8 = new LZ4BlockOutputStream(new FileOutputStream(baseName + ".pbf.lz4"));
			writePbf(population, output8);
			output8.close();
			long end8 = System.currentTimeMillis();

			log.info("[" + loop + "]  pbf.gz");
			long start9 = System.currentTimeMillis();
			OutputStream output9 = new GZIPOutputStream(new FileOutputStream(baseName + ".pbf.gz"));
			writePbf(population, output9);
			output9.close();
			long end9 = System.currentTimeMillis();

			log.info("[" + loop + "]  avro");
			long start10 = System.currentTimeMillis();
			AvroPersonWriter writer10 = new AvroPersonWriter(new File(baseName + ".avro"));
			for (Person person : population.getPersons().values()) {
				writer10.append(person);
			}
			writer10.close();
			long end10 = System.currentTimeMillis();


			System.out.println("[" + loop + "] times:");
			System.out.println("[" + loop + "] xml      = " + (end1 - start1)/1000.0);
			System.out.println("[" + loop + "] xml.gz   = " + (end2 - start2)/1000.0);
			System.out.println("[" + loop + "] kryo     = " + (end3 - start3)/1000.0);
			System.out.println("[" + loop + "] kryo.lz4 = " + (end4 - start4)/1000.0);
//			System.out.println("[" + loop + "] kryo.cst = " + (end5 - start5)/1000.0);
//			System.out.println("[" + loop + "] fst      = " + (end6 - start6)/1000.0);
			System.out.println("[" + loop + "] pbf      = " + (end7 - start7)/1000.0);
			System.out.println("[" + loop + "] pbf.lz4  = " + (end8 - start8)/1000.0);
			System.out.println("[" + loop + "] pbf.gz   = " + (end9 - start9)/1000.0);
			System.out.println("[" + loop + "] avro     = " + (end10 - start10)/1000.0);

			System.out.println("[" + loop + "] sizes:");
			System.out.println("[" + loop + "] xml      = " + new File(baseName + ".xml").length());
			System.out.println("[" + loop + "] xml.gz   = " + new File(baseName + ".xml.gz").length());
			System.out.println("[" + loop + "] kryo     = " + new File(baseName + ".kryo").length());
			System.out.println("[" + loop + "] kryo.lz4 = " + new File(baseName + ".kryo.lz4").length());
//			System.out.println("[" + loop + "] kryo.cst = " + new File(baseName + ".kryo.custom").length());
//			System.out.println("[" + loop + "] fst      = " + new File(baseName + ".fst").length());
			System.out.println("[" + loop + "] pbf      = " + new File(baseName + ".pbf").length());
			System.out.println("[" + loop + "] pbf.lz4  = " + new File(baseName + ".pbf.lz4").length());
			System.out.println("[" + loop + "] pbf.gz   = " + new File(baseName + ".pbf.gz").length());
			System.out.println("[" + loop + "] avro     = " + new File(baseName + ".avro").length());
		}
	}

	private static void writePbf(Population population, OutputStream output) throws IOException {
		for (Person person : population.getPersons().values()) {

			PopulationPBF.Person.Builder pbfPerson = PopulationPBF.Person.newBuilder();
			pbfPerson.setId(person.getId().toString());
			for (Plan plan : person.getPlans()) {
				PopulationPBF.Plan.Builder pbfPlan = PopulationPBF.Plan.newBuilder();
				pbfPlan.setScore(plan.getScore());
				pbfPlan.setSelected(person.getSelectedPlan() == plan);
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity act = (Activity) planElement;
						PopulationPBF.Activity.Builder pbfAct = PopulationPBF.Activity.newBuilder();
						pbfAct.setType(act.getType());
						if (act.getStartTime() != Time.UNDEFINED_TIME) {
							pbfAct.setStartTime(act.getStartTime());
						}
						if (act.getEndTime() != Time.UNDEFINED_TIME) {
							pbfAct.setEndTime(act.getEndTime());
						}
						if (act.getMaximumDuration() != Time.UNDEFINED_TIME) {
							pbfAct.setMaxDur(act.getMaximumDuration());
						}
						if (act.getFacilityId() != null) {
							pbfAct.setFacility(act.getFacilityId().toString());
						}
						if (act.getLinkId() != null) {
							pbfAct.setLink(act.getLinkId().toString());
						}
						if (act.getCoord() != null) {
							pbfAct.setX(act.getCoord().getX());
							pbfAct.setY(act.getCoord().getY());
							if (act.getCoord().hasZ()) {
								pbfAct.setZ(act.getCoord().getZ());
							}
						}
						pbfPlan.addPlanElement(PopulationPBF.PlanElement.newBuilder().setAct(pbfAct).build());
					} else if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						PopulationPBF.Leg.Builder pbfLeg = PopulationPBF.Leg.newBuilder();
						pbfLeg.setMode(leg.getMode());
						if (leg.getDepartureTime() != Time.UNDEFINED_TIME) {
							pbfLeg.setDepTime(leg.getDepartureTime());
						}
						if (leg.getTravelTime() != Time.UNDEFINED_TIME) {
							pbfLeg.setTravTime(leg.getTravelTime());
						}
						if (leg.getRoute() != null) {
							Route route = leg.getRoute();
							PopulationPBF.Route.Builder pbfRoute = PopulationPBF.Route.newBuilder();
							pbfRoute.setDescription(route.getRouteDescription());
							pbfRoute.setType(route.getRouteType());
							if (route.getStartLinkId() != null) {
								pbfRoute.setStartLink(route.getStartLinkId().toString());
							}
							if (route.getEndLinkId() != null) {
								pbfRoute.setEndLink(route.getEndLinkId().toString());
							}
							if (route.getTravelTime() != Time.UNDEFINED_TIME) {
								pbfRoute.setTravTime(route.getTravelTime());
							}
							if (Double.isFinite(route.getDistance())) {
								pbfRoute.setDistance(route.getDistance());
							}
							pbfLeg.setRoute(pbfRoute);
						}
						pbfPlan.addPlanElement(PopulationPBF.PlanElement.newBuilder().setLeg(pbfLeg).build());
					}
				}

				pbfPerson.addPlan(pbfPlan);
			}
			pbfPerson.build().writeDelimitedTo(output);
		}
	}

	public static class PersonSerializer extends Serializer<Person> {

		@Override
		public void write(Kryo kryo, Output output, Person person) {
			output.writeString("boo");
		}

		@Override
		public Person read(Kryo kryo, Input input, Class<Person> aClass) {
			return null;
		}
	}
}
