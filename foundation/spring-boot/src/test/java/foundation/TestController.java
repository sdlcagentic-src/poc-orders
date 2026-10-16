package foundation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/ok")
    String ok() {
        return "ok";
    }

    @GetMapping("/needs-param")
    String needsParam(@RequestParam("name") String name) {
        return name;
    }

    @GetMapping("/boom")
    String boom() {
        throw new IllegalStateException("secret internal detail");
    }
}
