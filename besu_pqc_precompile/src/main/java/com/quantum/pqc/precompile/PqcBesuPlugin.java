package com.quantum.pqc.precompile;

import org.hyperledger.besu.plugin.BesuContext;
import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.services.PicoCLIOptions;

/**
 * Entry point Besu discovers via META-INF/services. Registering a custom
 * precompile through Besu's plugin API means MlDsaVerifyPrecompile ships as
 * a jar dropped in Besu's `plugins/` directory — no fork of Besu's source
 * tree required.
 *
 * Exact registration API (precompile registry service name/method) varies
 * by Besu version — check the org.hyperledger.besu.plugin.services
 * interfaces for the release you're running before wiring this up.
 */
public class PqcBesuPlugin implements BesuPlugin {

    private BesuContext context;

    @Override
    public void register(BesuContext context) {
        this.context = context;
        // TODO: fetch the precompile registry service from context, e.g.:
        //   context.getService(PrecompileContractRegistry.class)
        //          .ifPresent(registry -> registry.register(FIXED_ADDRESS, MlDsaVerifyPrecompile::new));
        // Confirm exact service/registry class name against the target Besu version.
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }
}
