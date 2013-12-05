package com.puppetlabs.jenkins.redis;

import hudson.Extension;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;

public class RedisParameterDefinition extends SimpleParameterDefinition
{
  public final String redisKey;
  public final String redisServer;

  @DataBoundConstructor
  public RedisParameterDefinition(String name, String redisKey, String redisServer, String description) {
    super(name, description);
    this.redisKey = redisKey;
    this.redisServer = redisServer;
  }

  public final String getDefaultValue() {
    Jedis redis = new Jedis(redisServer);
    return redis.get(redisKey);
  }

  @Override
  public ParameterValue getDefaultParameterValue() {
    String value = getDefaultValue();
    return new StringParameterValue(getName(), value);
  }

  @Override
  public ParameterValue createValue(String value) {
    return new StringParameterValue(getName(), value);
  }

  @Override
  public final ParameterValue createValue(StaplerRequest req, JSONObject jo)
  {
    final JSONObject parameterJsonModel = new JSONObject(false);
    final Object value = jo.get("value");
    final String valueAsText;
    if (JSONUtils.isArray(value)) 
    {
      valueAsText = ((JSONArray)value).join(",", true);
    } else 
    {
      valueAsText = String.valueOf(value);
    }
    parameterJsonModel.put("name",  jo.get("name"));
    parameterJsonModel.put("value", valueAsText);

    return req.bindJSON(StringParameterValue.class, parameterJsonModel);
  }

  @Extension
  public static final class DescriptorImpl extends ParameterDescriptor
  {
    private static final String DISPLAY_NAME = "DisplayName";

    @Override
    public final String getDisplayName()
    {
      return "Redis Parameter";
    }
  }
}
