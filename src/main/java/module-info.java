module com.pfstu.pixellauncher {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.commons.codec;
    requires java.net.http;


    opens com.pfstu.pixellauncher to javafx.fxml;
    exports com.pfstu.pixellauncher;
    exports com.pfstu.pixellauncher.Modules.Launch;
    exports com.pfstu.pixellauncher.Modules.Menu to javafx.fxml;
    opens com.pfstu.pixellauncher.Modules.Launch to javafx.fxml;
}