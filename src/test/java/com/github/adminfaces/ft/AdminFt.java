package com.github.adminfaces.ft;

import com.github.adminfaces.ft.pages.*;
import com.github.adminfaces.ft.pages.fragments.LeftMenu;
import com.github.adminfaces.ft.pages.fragments.SearchDialog;
import com.github.adminfaces.ft.util.Deployments;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.GrapheneElement;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.InitialPage;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.jboss.arquillian.graphene.Graphene.*;
import static org.junit.Assert.assertTrue;

/**
 * Car acceptance tests
 */

@RunWith(Arquillian.class)
public class AdminFt {

    @Deployment(name = "admin-starter.war", testable = false)
    public static Archive<?> createDeployment() {
        WebArchive war = Deployments.createDeployment();
        System.out.println(war.toString(true));
        return war;
    }

    @ArquillianResource
    URL url;

    @Drone
    WebDriver webDriver;

    @FindByJQuery("div[id='messages'] span.ui-messages-error-detail")
    private GrapheneElement errorMessages;

    @FindByJQuery("div[id='info-messages'] .ui-messages-info-detail")
    private GrapheneElement infoMessages;


    @Page
    private LogonPage logon;

    @Page
    private IndexPage index;

    @Page
    private CarListPage carList;

    @Page
    private CarFormPage carForm;

    @FindByJQuery("section.sidebar > ul.sidebar-menu")
    private LeftMenu menu;

    @FindByJQuery("div.ui-dialog.box-success")
    private SearchDialog searchDialog;


    @Test
    @InSequence(1)
    public void shouldLogonSuccessfully(@InitialPage LogonPage logon) {
        assertThat(logon.isPresent()).isTrue();
        logon.doLogon("abc@gmail.com", "abcde");
        assertThat(infoMessages.isPresent()).isTrue();
        assertThat(infoMessages.getText()).contains("Logged in successfully as abc@gmail.com");
    }

    @Test
    @InSequence(2)
    public void shouldListCars() {
       menu.listCars();
       assertThat(carList.isPresent()).isTrue();
    }

    @Test
    @InSequence(3)
    public void shouldPaginateCars() {
        carList.paginate();
        carList.getDatatable().findGrapheneElements(By.cssSelector("a.ui-link"))
                .forEach(e -> assertTrue(e.getText().equals("model 46") ||
                        e.getText().equals("model 47") ||  e.getText().equals("model 48")
                        ||  e.getText().equals("model 49") ||  e.getText().equals("model 50")));
    }


    @Test
    @InSequence(4)
    public void shouldFilterByModel() {
        carList.filterByModel("model 8");
        waitModel(webDriver).withTimeout(3,TimeUnit.SECONDS);
        assertThat(carList.getTableRows().get(0).getText()).contains("model 8");
    }

    @Test
    @InSequence(5)
    public void shouldRemoveMultipleCars() {
        waitModel(webDriver);
        carList.clear();
        waitModel(webDriver);
        webDriver.findElements(By.cssSelector("td .ui-chkbox-box")).forEach(e -> e.click());
        waitModel();
        carList.remove();
        assertThat(infoMessages.getText()).isEqualTo("5 cars deleted successfully!");
    }

    @Test
    @InSequence(6)
    public void shouldEditViaDatatable() {
        waitModel().withTimeout(5,TimeUnit.SECONDS).until()
                .element(carList.getConfirmHeader()).is().not().visible();
        carList.filterByModel("model 20");
        waitModel(webDriver);
        guardHttp(webDriver.findElement(By.cssSelector("td[role=gridcell] a"))).click();
        assertThat(carForm.isPresent()).isTrue();
        carForm.getInputModel().clear();
        waitGui(webDriver);
        carForm.getInputModel().sendKeys("model edit");
        carForm.save();
        assertThat(infoMessages.getText()).isEqualTo("Car model edit updated successfully");
    }

    @Test
    @InSequence(7)
    public void shouldEditViaUrl() {
        menu.goHome();
        webDriver.get(url+"/car-form.xhtml?id=20");
        assertThat(carForm.isPresent()).isTrue();
        carForm.getInputModel().clear();
        waitGui(webDriver);
        carForm.getInputModel().sendKeys("model 20 edit");
        carForm.save();
        assertThat(infoMessages.getText()).isEqualTo("Car model 20 edit updated successfully");
    }

    @Test
    @InSequence(8)
    @Ignore("yes button from confirm dialog is not enabled")
    public void shouldRemoveCar() {
        waitModel(webDriver);
        carForm.remove();
        assertThat(infoMessages.getText()).isEqualTo("Car model 20 edit removed successfully");
    }

    @Test
    @InSequence(9)
    public void shouldInsertCar(@InitialPage CarListPage carList) {
        carList.newCar();
        waitModel().until().element(carForm.getInputModel()).is().present();
        carForm.getInputModel().sendKeys("new model");
        carForm.getInputName().sendKeys("new name");
        carForm.getInputPrice().sendKeys("1.5");
        carForm.save();
        assertThat(infoMessages.getText()).isEqualTo("Car new model created successfully");
    }

    @Test
    @InSequence(10)
    public void shouldSearchCarByNameAndPrice(@InitialPage CarListPage carList) {
        carList.search();
        searchDialog.getName().sendKeys("name1");
        searchDialog.getMinPrice().sendKeys("16");
        searchDialog.getMaxPrice().sendKeys("17.8");
        searchDialog.search();
        searchDialog.close();
        assertThat(carList.getTableRows()).hasSize(2);
        assertThat(carList.getTableRows().get(0).getText()).contains("name16");
        assertThat(carList.getTableRows().get(1).getText()).contains("name17");

    }

    @Test
    @InSequence(99)
    public void shouldLogout() {
        webDriver.findElement(By.id("userImage")).click();
        waitModel().until().element(By.cssSelector("li.open")).is().present();
        webDriver.findElement(By.id("logout")).click();
        waitModel().until().element(logon.getLoginBox()).is().present();
    }


}
