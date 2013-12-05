package com.puppetlabs.jenkins.redis;

import hudson.Extension;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;

@SuppressWarnings({"unchecked"})
public class RedisNotifier extends Notifier {
  private static final Logger logger = Logger.getLogger(RedisNotifier.class.getName());

  public final String redisServerUrl;
  public final String exportedVariables;
  public final String redisNamespace;

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    Jedis redis = new Jedis(redisServerUrl);
    EnvVars environment = build.getEnvironment(listener);
    String[] vars = exportedVariables.split(",");
    String ns = environment.expand(redisNamespace);
    for(int i = 0; i < vars.length; i++) {
      String var = vars[i].trim();
      String key = ns + var;
      String val = environment.get(var);
      redis.set(key, val);
    }
    return true;
  }

  @DataBoundConstructor
  public RedisNotifier(final String redisServerUrl, final String exportedVariables, final String redisNamespace) {
    this.redisServerUrl = redisServerUrl;
    this.exportedVariables = exportedVariables;
    this.redisNamespace = redisNamespace;
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    public String getDisplayName() {
      return "Publish jenkins environment to a Redis server";
    }
  }
}
