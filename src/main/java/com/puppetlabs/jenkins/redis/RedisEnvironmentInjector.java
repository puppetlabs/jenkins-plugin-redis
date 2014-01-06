package com.puppetlabs.jenkins.redis;

import hudson.Extension;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.model.*;
import org.kohsuke.stapler.DataBoundConstructor;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.logging.Logger;
import java.io.IOException;

public class RedisEnvironmentInjector extends Builder {
  public final String redisServerUrl;
  public final String exportedVariables;
  public final String redisNamespace;

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    Logger logger = Logger.getLogger("redis");
    Jedis redis = new Jedis(redisServerUrl);
    EnvVars environment = build.getEnvironment(listener);
    String[] vars = exportedVariables.split(",");
    String ns = environment.expand(redisNamespace);

    ArrayList<ParameterValue> values = new ArrayList<ParameterValue>();
    HashSet<String> names = new HashSet<String>();
    for(int i = 0; i < vars.length; i++) {
      String var = vars[i].trim();
      String key = ns + var;
      String current_val = environment.get(var);
      logger.warning("Fetching a new var");
      logger.warning(var);
      if(current_val == null || current_val.isEmpty()) {
        String val = redis.get(key);
        values.add(new StringParameterValue(var, val));
        names.add(var);
        logger.warning("Got a value: ");
        logger.warning(val);
      } else {
        logger.warning("Has a value: ");
        logger.warning(current_val);
      }
    }

    ParametersAction old_params = build.getAction(ParametersAction.class);
    ParametersAction new_params = null;
    if(old_params != null) {
      List<ParameterValue> old_values = old_params.getParameters();
      for(ParameterValue v : old_values) {
        if (!names.contains(v.getName())) {
          values.add(v);
        }
      }
    }
    new_params = new ParametersAction(values);

    build.getActions().remove(old_params);  
    build.addAction(new_params);
    return true;
  }

  @DataBoundConstructor
  public RedisEnvironmentInjector(final String redisServerUrl, final String exportedVariables, final String redisNamespace) {
    this.redisServerUrl = redisServerUrl;
    this.exportedVariables = exportedVariables;
    this.redisNamespace = redisNamespace;
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    public String getDisplayName() {
      return "Augment jenkins environment from a Redis server";
    }
  }
}
