package academy.devdojo.webflux.controller;

import academy.devdojo.webflux.GlobalTestConfig;
import academy.devdojo.webflux.databuilder.AnimeCreatorBuilder;
import academy.devdojo.webflux.entity.Anime;
import academy.devdojo.webflux.exception.CustomAttributes;
import academy.devdojo.webflux.service.AnimeService;
import io.restassured.http.ContentType;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.context.WebApplicationContext;
import reactor.blockhound.BlockingOperationError;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import static academy.devdojo.webflux.databuilder.AnimeCreatorBuilder.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

//@WebFluxTest(controllers = AnimeController.class)
//@Import({AnimeService.class ,CustomAttributes.class})
////@AutoConfigureWebTestClient
//@SpringBootTest
public class AnimeControllerIntegrTest extends GlobalTestConfig {

    @Autowired
    WebTestClient testClient;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private Anime anime_1, anime_2;

    @Before
    public void setUpLocal() {
        anime_1 = animeWithName().create();
        anime_2 = animeWithName().create();
        testClient = WebTestClient.bindToServer().baseUrl("http://localhost:8080/animes").build();
    }

    @After
    public void tearDownLocal() {
//        service.delete(anime.getId());
    }

    @Test
    public void save() {

        RestAssuredWebTestClient.given()
                .webTestClient(testClient)
                .header("Accept" ,ContentType.ANY)
                .header("Content-type" ,ContentType.JSON)
                .body(anime_1)

                .when()
                .post()

                .then()
                .log().headers().and()
                .log().body().and()
                .contentType(ContentType.JSON)
                .statusCode(CREATED.value())

                //equalTo para o corpo do Json
                .body("name" ,equalTo(anime_1.getName()))
        ;
    }

    @Test
    public void saveall_transaction_rollback() {

        List<Anime> listAnime = Arrays.asList(anime_1 ,anime_2);

        RestAssuredWebTestClient.given()
                .webTestClient(testClient)
                .header("Accept" ,ContentType.ANY)
                .body(listAnime)

                .when()
                .post("/saveall_rollback")

                .then()
                .contentType(ContentType.JSON)
                .statusCode(CREATED.value())
                .log().headers().and()
                .log().body().and()

                .body("size()" ,is(listAnime.size()))
                .body("name" ,hasItems(anime_1.getName() ,anime_2.getName()))
        ;
    }

    @Test
    public void saveall_transaction_rollback_ERROR() {

        List<Anime> listAnime = Arrays.asList(anime_1 ,anime_2.withName(""));

        RestAssuredWebTestClient.given()
                .webTestClient(testClient)
                .header("Accept" ,ContentType.ANY)
                .body(listAnime)

                .when()
                .post("/saveall_rollback")

                .then()
                .contentType(ContentType.JSON)
                .statusCode(BAD_REQUEST.value())
                .log().headers().and()
                .log().body().and()

                .body("developerMensagem",is("A ResponseStatusException happened!!!"))
        ;
    }

    @Test
    public void saveTestEmpty() {
        anime_1 = animeEmpty().create();

        RestAssuredWebTestClient.given()
                .webTestClient(testClient)
                .header("Accept" ,ContentType.ANY)
                .header("Content-type" ,ContentType.JSON)
                .body(anime_1)

                .when()
                .post()

                .then()
                .statusCode(BAD_REQUEST.value())

                .body("developerMensagem" ,equalTo("A ResponseStatusException happened!!!"))
        ;
    }

    @Test
    public void get() {

        RestAssuredWebTestClient
                .given()
                .webTestClient(testClient)

                .when()
                .get()

                .then()
                .statusCode(OK.value())
                .log().headers().and()
                .log().body().and()

                .body("name" ,hasItem("GLAUCO"))
        ;
    }

    @Test
    public void getById() {

        RestAssuredWebTestClient
                .given()
                .webTestClient(testClient)

                .when()
                .get("/{id}" ,"1")

                .then()
                .statusCode(OK.value())

                .body("name" ,is("paulo"))
        ;
    }

    @Test
    public void getById_ERROR() {

        RestAssuredWebTestClient
                .given()
                .webTestClient(testClient)

                .when()
                .get("/{id}" ,"100")

                .then()
                .statusCode(NOT_FOUND.value())

                .body("developerMensagem" ,equalTo("A ResponseStatusException happened!!!"))
                .body("name" ,nullValue())
        ;
    }

    @Test
    public void delete() {
        RestAssuredWebTestClient
                .given()
                .webTestClient(testClient)

                .when()
                .delete("/{id}" ,"1")

                .then()
                .statusCode(NO_CONTENT.value())
        ;
    }

    @Test
    public void delete_ERROR() {

        RestAssuredWebTestClient
                .given()
                .webTestClient(testClient)

                .when()
                .delete("/{id}" ,"100")

                .then()
                .statusCode(NOT_FOUND.value())

                .body("developerMensagem" ,equalTo("A ResponseStatusException happened!!!"))
                .body("name" ,nullValue())
        ;
    }

    @Test
    public void update() {

        RestAssuredWebTestClient.given()
                .webTestClient(testClient)
                .body(anime_1)

                .when()
                .put("/{id}" ,"3")

                .then()
                .log().headers().and()
                .log().body().and()
                .statusCode(NO_CONTENT.value())
        ;
    }

    @Test
    public void update_Empty() {

        RestAssuredWebTestClient.given()
                .webTestClient(testClient)
                .body(anime_1)

                .when()
                .put("/{id}" ,"300")

                .then()
                .statusCode(NOT_FOUND.value())

                .body("developerMensagem" ,equalTo("A ResponseStatusException happened!!!"))
                .body("name" ,nullValue())
        ;
    }


    @Test
    public void blockHoundWorks() {
        try {
            FutureTask<?> task = new FutureTask<>(() -> {
                Thread.sleep(0);
                return "";
            });

            Schedulers.parallel().schedule(task);

            task.get(10 ,TimeUnit.SECONDS);
            Assert.fail("should fail");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Assert.assertTrue("detected" ,e.getCause() instanceof BlockingOperationError);
        }
    }

    //--------------------------------------------------------------------------
    @Test
    public void get_Webcontext() {
        //        testClient = WebTestClient.bindToController(controller).build();
        testClient = WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .build();

        RestAssuredWebTestClient
                .given()
                .webTestClient(testClient)

                .when()
                .get()

                .then()
                .statusCode(OK.value())

                .body("name" ,hasItem("GLAUCO"))
        ;
    }

    @Test
    public void get_StandAlone() {
//        RestAssuredWebTestClient.standaloneSetup(new AnimeController());
        RestAssuredWebTestClient.webAppContextSetup(webApplicationContext);
        RestAssuredWebTestClient
                .given()
//                .standaloneSetup(new AnimeController())

                .when()
                .get()

                .then()
                .statusCode(OK.value())

                .body("name" ,hasItem("paulo"))
        ;
    }
}

/* JAMAIS DELETAR!!!!!!!

java.lang.IllegalStateException: You haven't configured a WebTestClient instance. You can do this statically

RestAssuredWebTestClient.mockMvc(..)
RestAssuredWebTestClient.standaloneSetup(..);
RestAssuredWebTestClient.webAppContextSetup(..);

or using the DSL:

given().
		mockMvc(..). ..
 */
