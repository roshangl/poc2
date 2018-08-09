package map.poc2.controller;

import map.poc2.model.*;
import map.poc2.service.BuyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/buy")
public class BuyController {

    @Autowired
    private BuyService buyService;

    @PostMapping("/initialize")
    public ExecutionTimeResult intializeModel(@RequestBody IntializeArgs intializeArgs) {
        return buyService.initializeModel(intializeArgs);
    }

    @PostMapping("applyRules")
    public ExecutionTimeResult applyRules(){
        return buyService.applyRules();
    }

    @PostMapping("/set")
    public ExecutionTimeResult setModel(@RequestBody SetArgs setArgs) {
        return buyService.setModel(setArgs);
    }

    @PostMapping("/get")
    public GetResult getModel(@RequestBody GetArgs getArgs) {
        return buyService.getModel(getArgs);
    }

    @PostMapping("/getAll")
    public GetResult getAllModel(@RequestBody SetArgs setArgs) {
        return buyService.getAllModel(setArgs);
    }
}
