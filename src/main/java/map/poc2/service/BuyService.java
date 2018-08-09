package map.poc2.service;

import map.poc2.model.*;

public interface BuyService {

    ExecutionTimeResult initializeModel(IntializeArgs intializeArgs);

    ExecutionTimeResult setModel(SetArgs setArgs);

    GetResult getAllModel(SetArgs setArgs);

    GetResult getModel(GetArgs getArgs);

    ExecutionTimeResult applyRules();

    //GetResult applyRules();
}
