package com.tlnt.engine.particle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

public final class ParticleEffect {
   public static ObjectMap<Integer, ParticleEffect> effectsById = new ObjectMap<>();
   public int id;
   public String name;
   public final Vector2 position = new Vector2();
   public final Vector2 renderOffset = new Vector2();
   public float scale = 1.0F;
   public boolean flipX;
   public Vector2 parallax = new Vector2();
   public Array<ParticleEmitter> emitters = new Array<>();
   public long lastUpdateFrameId;

   public static void clearRegistry() {
      effectsById.clear();
   }

   public static void register(ParticleEffect effect) {
      effectsById.put(effect.id, effect);
   }

   public static ParticleEffect getById(int id) {
      return effectsById.get(id);
   }

   public ParticleEffect() {
   }

   public ParticleEffect(ParticleEffect source) {
      this.id = source.id;
      this.name = source.name;

      for (ParticleEmitter emitter : source.emitters) {
         this.emitters.add(new ParticleEmitter(this, emitter));
      }
   }

   public static int readInt(JsonValue json, String key, int defaultValue) {
      JsonValue value;
      return (value = json.get(key)) != null && !value.isNull() ? value.asInt() : defaultValue;
   }

   public static float readFloat(JsonValue json, String key, float defaultValue) {
      JsonValue value;
      return (value = json.get(key)) != null && !value.isNull() ? value.asFloat() : defaultValue;
   }

   public static boolean readBool(JsonValue json, String key, boolean defaultValue) {
      JsonValue value;
      return (value = json.get(key)) != null && !value.isNull() ? value.asBoolean() : defaultValue;
   }

   public static String readString(JsonValue json, String key, String defaultValue) {
      if ((json = json.get(key)) != null && !json.isNull()) {
         return json.type() == JsonValue.ValueType.stringValue ? json.asString() : String.valueOf(json.asDouble());
      } else {
         return defaultValue;
      }
   }

   public static void readCurvePoints(JsonValue json, Array<CurvePoint> points) {
      points.clear();
      if (json != null && json.isArray()) {
         int index = 0;
         JsonValue.JsonIterator iter = json.iterator();

         while (iter.hasNext()) {
            JsonValue entry;
            if ((entry = (JsonValue)iter.next()).isArray()) {
               if (entry.size >= 2) {
                  float x = entry.get(0).asFloat();
                  float y = entry.get(1).asFloat();
                  int notFirst;
                  if (index != 0) {
                     notFirst = 1;
                  } else {
                     notFirst = 0;
                  }

                  CurvePoint created = new CurvePoint(x, y, (notFirst != 0));
                  CurvePoint point = created;
                  if (entry.size >= 6) {
                     point.handleOutX = entry.get(2).asFloat();
                     point.handleOutY = entry.get(3).asFloat();
                     point.handleInX = entry.get(4).asFloat();
                     point.handleInY = entry.get(5).asFloat();
                  }

                  points.add(point);
               }

               index++;
            }
         }
      }
   }

   public final void setPosition(float x, float y) {
      Vector2 oldPosition = new Vector2(this.position);
      this.position.set(x, y);
      Vector2 offset = new Vector2(this.position).sub(oldPosition);
      Array.ArrayIterator emitterIter = this.emitters.iterator();

      while (emitterIter.hasNext()) {
         ParticleEmitter emitter;
         if ((emitter = (ParticleEmitter)emitterIter.next()).attached) {
            Array.ArrayIterator particleIter = emitter.particles.iterator();

            while (particleIter.hasNext()) {
               ((Particle)particleIter.next()).translate(offset);
            }
         }
      }
   }

   public final Vector2 getPosition() {
      return this.position;
   }

   public final Vector2 getRenderOffset() {
      return this.renderOffset;
   }

   public final void setScale(float scale) {
      this.scale = scale;
   }

   public final float getScale() {
      return this.scale;
   }

   public final void setFlipX(boolean flipX) {
      this.flipX = flipX;
   }

   public final boolean isFlipX() {
      return this.flipX;
   }

   public final void setParallax(float x, float y) {
      this.parallax.set(x, y);
   }

   public final Vector2 getParallax() {
      return this.parallax;
   }

   public final void update(float delta) {
      if (this.lastUpdateFrameId != Gdx.graphics.getFrameId()) {
         this.lastUpdateFrameId = Gdx.graphics.getFrameId();
         Array.ArrayIterator iter = this.emitters.iterator();

         while (iter.hasNext()) {
            ((ParticleEmitter)iter.next()).update(delta);
         }
      }
   }

   public final void loadFromJson(String json) {
      if (json != null && !json.isEmpty()) {
         JsonValue root;
         try {
            root = new JsonReader().parse(json);
         } catch (RuntimeException e) {
            return;
         }

         if (root != null && root.isObject()) {
            this.id = readInt(root, "id", this.id);
            this.name = readString(root, "name", this.name != null ? this.name : "");
            this.emitters.clear();
            JsonValue emittersJson;
            if ((emittersJson = root.get("emitters")) != null && emittersJson.isArray()) {
               JsonValue.JsonIterator emitterIter = emittersJson.iterator();

               while (emitterIter.hasNext()) {
                  JsonValue emitterJson;
                  if ((emitterJson = (JsonValue)emitterIter.next()).isObject()) {
                     ParticleEmitter emitter;
                     ParticleEmitter created = emitter = new ParticleEmitter(this);
                     created.name = readString(emitterJson, "name", emitter.name);
                     emitter.active = readBool(emitterJson, "active", emitter.active);
                     JsonValue value;
                     int modeIndex;
                     if ((value = emitterJson.get("mode")) != null
                        && !value.isNull()
                        && (modeIndex = (int)value.asDouble()) >= 0
                        && modeIndex < ParticleEmitter.SpriteMode.getValues().length) {
                        emitter.spriteMode = ParticleEmitter.SpriteMode.getValues()[modeIndex];
                     }

                     emitter.selectedImageIndex = readInt(emitterJson, "imageSelected", emitter.selectedImageIndex);
                     emitter.animationFps = readInt(emitterJson, "animationFps", emitter.animationFps);
                     if ((value = emitterJson.get("images")) != null && value.isArray()) {
                        JsonValue.JsonIterator imageIter = value.iterator();

                        while (imageIter.hasNext()) {
                           JsonValue imageJson;
                           if ((imageJson = (JsonValue)imageIter.next()) != null && !imageJson.isNull()) {
                              if (imageJson.type() == JsonValue.ValueType.stringValue) {
                                 emitter.imagePaths.add(imageJson.asString());
                              } else {
                                 emitter.imagePaths.add(String.valueOf(imageJson.asDouble()));
                              }
                           }
                        }
                     }

                     emitter.lowDuration = readFloat(emitterJson, "lowDuration", emitter.lowDuration);
                     emitter.highDuration = readFloat(emitterJson, "highDuration", emitter.highDuration);
                     emitter.useHighDuration = readBool(emitterJson, "useHighDuration", emitter.useHighDuration);
                     emitter.useHighDelay = readBool(emitterJson, "useHighDelay", emitter.useHighDelay);
                     emitter.lowDelay = readFloat(emitterJson, "lowDelay", emitter.lowDelay);
                     emitter.highDelay = readFloat(emitterJson, "highDelay", emitter.highDelay);
                     emitter.attached = readBool(emitterJson, "attached", emitter.attached);
                     emitter.premultipliedAlpha = readBool(emitterJson, "premultipliedAlpha", emitter.premultipliedAlpha);
                     emitter.additive = readBool(emitterJson, "additive", emitter.additive);
                     emitter.continuous = readBool(emitterJson, "continuous", emitter.continuous);
                     emitter.useCurves = readBool(emitterJson, "useCurves", emitter.useCurves);
                     emitter.useCurvesEmission = readBool(emitterJson, "useCurvesEmission", emitter.useCurves);
                     emitter.useCurvesLife = readBool(emitterJson, "useCurvesLife", emitter.useCurves);
                     emitter.useCurvesRotation = readBool(emitterJson, "useCurvesRotation", emitter.useCurves);
                     emitter.useCurvesVelocity = readBool(emitterJson, "useCurvesVelocity", emitter.useCurves);
                     emitter.useCurvesAngle = readBool(emitterJson, "useCurvesAngle", emitter.useCurves);
                     emitter.useCurvesGravity = readBool(emitterJson, "useCurvesGravity", emitter.useCurves);
                     emitter.useCurvesWind = readBool(emitterJson, "useCurvesWind", emitter.useCurves);
                     emitter.useCurvesSizeX = readBool(emitterJson, "useCurvesSizeX", emitter.useCurves);
                     emitter.useCurvesSizeY = readBool(emitterJson, "useCurvesSizeY", emitter.useCurves);
                     emitter.useCurvesTrailMaxPoints = readBool(emitterJson, "useCurvesTrailMaxPoints", emitter.useCurves);
                     emitter.useCurvesTrailStepTime = readBool(emitterJson, "useCurvesTrailStepTime", emitter.useCurves);
                     emitter.useCurvesTrailAlpha = readBool(emitterJson, "useCurvesTrailAlpha", emitter.useCurves);
                     emitter.useCurvesBloomIntensity = readBool(emitterJson, "useCurvesBloomIntensity", emitter.useCurves);
                     emitter.useCurvesBloomSizeMultiplier = readBool(emitterJson, "useCurvesBloomSizeMultiplier", emitter.useCurves);
                     emitter.useCurvesLineWidth = readBool(emitterJson, "useCurvesLineWidth", emitter.useCurves);
                     emitter.useCurvesLineHeight = readBool(emitterJson, "useCurvesLineHeight", emitter.useCurves);
                     emitter.useCurvesSquareWidth = readBool(emitterJson, "useCurvesSquareWidth", emitter.useCurves);
                     emitter.useCurvesSquareHeight = readBool(emitterJson, "useCurvesSquareHeight", emitter.useCurves);
                     emitter.useCurvesTransparency = readBool(emitterJson, "useCurvesTransparency", emitter.useCurves);
                     emitter.trailEnabled = readBool(emitterJson, "trailEnabled", emitter.trailEnabled);
                     emitter.useLowTrailMaxPoints2 = readBool(emitterJson, "useLowTrailMaxPoints2", emitter.useLowTrailMaxPoints2);
                     emitter.useHighTrailMaxPoints2 = readBool(emitterJson, "useHighTrailMaxPoints2", emitter.useHighTrailMaxPoints2);
                     emitter.lowTrailMaxPoints1 = readInt(emitterJson, "lowTrailMaxPoints1", emitter.lowTrailMaxPoints1);
                     emitter.lowTrailMaxPoints2 = readInt(emitterJson, "lowTrailMaxPoints2", emitter.lowTrailMaxPoints2);
                     emitter.highTrailMaxPoints1 = readInt(emitterJson, "highTrailMaxPoints1", emitter.highTrailMaxPoints1);
                     emitter.highTrailMaxPoints2 = readInt(emitterJson, "highTrailMaxPoints2", emitter.highTrailMaxPoints2);
                     readCurvePoints(emitterJson.get("durationValuesTrailMaxPoints"), emitter.trailMaxPointsCurve);
                     emitter.useLowTrailStepTime2 = readBool(emitterJson, "useLowTrailStepTime2", emitter.useLowTrailStepTime2);
                     emitter.useHighTrailStepTime2 = readBool(emitterJson, "useHighTrailStepTime2", emitter.useHighTrailStepTime2);
                     emitter.lowTrailStepTime1 = readFloat(emitterJson, "lowTrailStepTime1", emitter.lowTrailStepTime1);
                     emitter.lowTrailStepTime2 = readFloat(emitterJson, "lowTrailStepTime2", emitter.lowTrailStepTime2);
                     emitter.highTrailStepTime1 = readFloat(emitterJson, "highTrailStepTime1", emitter.highTrailStepTime1);
                     emitter.highTrailStepTime2 = readFloat(emitterJson, "highTrailStepTime2", emitter.highTrailStepTime2);
                     readCurvePoints(emitterJson.get("durationValuesTrailStepTime"), emitter.trailStepTimeCurve);
                     emitter.useLowTrailAlpha2 = readBool(emitterJson, "useLowTrailAlpha2", emitter.useLowTrailAlpha2);
                     emitter.useHighTrailAlpha2 = readBool(emitterJson, "useHighTrailAlpha2", emitter.useHighTrailAlpha2);
                     emitter.lowTrailAlpha1 = readFloat(emitterJson, "lowTrailAlpha1", emitter.lowTrailAlpha1);
                     emitter.lowTrailAlpha2 = readFloat(emitterJson, "lowTrailAlpha2", emitter.lowTrailAlpha2);
                     emitter.highTrailAlpha1 = readFloat(emitterJson, "highTrailAlpha1", emitter.highTrailAlpha1);
                     emitter.highTrailAlpha2 = readFloat(emitterJson, "highTrailAlpha2", emitter.highTrailAlpha2);
                     readCurvePoints(emitterJson.get("durationValuesTrailAlpha"), emitter.trailAlphaCurve);
                     emitter.bloomEnabled = readBool(emitterJson, "bloomEnabled", emitter.bloomEnabled);
                     emitter.useLowBloomIntensity2 = readBool(emitterJson, "useLowBloomIntensity2", emitter.useLowBloomIntensity2);
                     emitter.useHighBloomIntensity2 = readBool(emitterJson, "useHighBloomIntensity2", emitter.useHighBloomIntensity2);
                     emitter.lowBloomIntensity1 = readFloat(emitterJson, "lowBloomIntensity1", emitter.lowBloomIntensity1);
                     emitter.lowBloomIntensity2 = readFloat(emitterJson, "lowBloomIntensity2", emitter.lowBloomIntensity2);
                     emitter.highBloomIntensity1 = readFloat(emitterJson, "highBloomIntensity1", emitter.highBloomIntensity1);
                     emitter.highBloomIntensity2 = readFloat(emitterJson, "highBloomIntensity2", emitter.highBloomIntensity2);
                     readCurvePoints(emitterJson.get("durationValuesBloomIntensity"), emitter.bloomIntensityCurve);
                     emitter.useLowBloomSizeMultiplier2 = readBool(emitterJson, "useLowBloomSizeMultiplier2", emitter.useLowBloomSizeMultiplier2);
                     emitter.useHighBloomSizeMultiplier2 = readBool(emitterJson, "useHighBloomSizeMultiplier2", emitter.useHighBloomSizeMultiplier2);
                     emitter.lowBloomSizeMultiplier1 = readFloat(emitterJson, "lowBloomSizeMultiplier1", emitter.lowBloomSizeMultiplier1);
                     emitter.lowBloomSizeMultiplier2 = readFloat(emitterJson, "lowBloomSizeMultiplier2", emitter.lowBloomSizeMultiplier2);
                     emitter.highBloomSizeMultiplier1 = readFloat(emitterJson, "highBloomSizeMultiplier1", emitter.highBloomSizeMultiplier1);
                     emitter.highBloomSizeMultiplier2 = readFloat(emitterJson, "highBloomSizeMultiplier2", emitter.highBloomSizeMultiplier2);
                     readCurvePoints(emitterJson.get("durationValuesBloomSizeMultiplier"), emitter.bloomSizeMultiplierCurve);
                     emitter.minParticleCount = readInt(emitterJson, "minCountAtAllTimes", emitter.minParticleCount);
                     emitter.maxParticleCount = readInt(emitterJson, "maxCountAtAllTimes", emitter.maxParticleCount);
                     emitter.useLowEmission2 = readBool(emitterJson, "useLowEmission2", emitter.useLowEmission2);
                     emitter.useHighEmission2 = readBool(emitterJson, "useHighEmission2", emitter.useHighEmission2);
                     emitter.lowEmission1 = readInt(emitterJson, "lowEmission1", emitter.lowEmission1);
                     emitter.lowEmission2 = readInt(emitterJson, "lowEmission2", emitter.lowEmission2);
                     emitter.highEmission1 = readInt(emitterJson, "highEmission1", emitter.highEmission1);
                     emitter.highEmission2 = readInt(emitterJson, "highEmission2", emitter.highEmission2);
                     readCurvePoints(emitterJson.get("durationValuesEmission"), emitter.emissionCurve);
                     emitter.useHighRotation2 = readBool(emitterJson, "useHighRotation2", emitter.useHighRotation2);
                     emitter.useLowRotation2 = readBool(emitterJson, "useLowRotation2", emitter.useLowRotation2);
                     emitter.highRotation1 = readFloat(emitterJson, "highRotation1", emitter.highRotation1);
                     emitter.highRotation2 = readFloat(emitterJson, "highRotation2", emitter.highRotation2);
                     emitter.lowRotation1 = readFloat(emitterJson, "lowRotation1", emitter.lowRotation1);
                     emitter.lowRotation2 = readFloat(emitterJson, "lowRotation2", emitter.lowRotation2);
                     readCurvePoints(emitterJson.get("durationValuesRotation"), emitter.rotationCurve);
                     emitter.highVelocity1 = readFloat(emitterJson, "highVelocity1", emitter.highVelocity1);
                     emitter.highVelocity2 = readFloat(emitterJson, "highVelocity2", emitter.highVelocity2);
                     emitter.useHighVelocity2 = readBool(emitterJson, "useHighVelocity2", emitter.useHighVelocity2);
                     emitter.lowVelocity1 = readFloat(emitterJson, "lowVelocity1", emitter.lowVelocity1);
                     emitter.lowVelocity2 = readFloat(emitterJson, "lowVelocity2", emitter.lowVelocity2);
                     emitter.useLowVelocity2 = readBool(emitterJson, "useLowVelocity2", emitter.useLowVelocity2);
                     readCurvePoints(emitterJson.get("durationValuesVelocity"), emitter.velocityCurve);
                     emitter.highAngle1 = readFloat(emitterJson, "highAngle1", emitter.highAngle1);
                     emitter.highAngle2 = readFloat(emitterJson, "highAngle2", emitter.highAngle2);
                     emitter.useHighAngle2 = readBool(emitterJson, "useHighAngle2", emitter.useHighAngle2);
                     emitter.lowAngle1 = readFloat(emitterJson, "lowAngle1", emitter.lowAngle1);
                     emitter.lowAngle2 = readFloat(emitterJson, "lowAngle2", emitter.lowAngle2);
                     emitter.useLowAngle2 = readBool(emitterJson, "useLowAngle2", emitter.useLowAngle2);
                     readCurvePoints(emitterJson.get("durationValuesAngle"), emitter.angleCurve);
                     emitter.highGravity1 = readFloat(emitterJson, "highGravity1", emitter.highGravity1);
                     emitter.highGravity2 = readFloat(emitterJson, "highGravity2", emitter.highGravity2);
                     emitter.useHighGravity2 = readBool(emitterJson, "useHighGravity2", emitter.useHighGravity2);
                     emitter.lowGravity1 = readFloat(emitterJson, "lowGravity1", emitter.lowGravity1);
                     emitter.lowGravity2 = readFloat(emitterJson, "lowGravity2", emitter.lowGravity2);
                     emitter.useLowGravity2 = readBool(emitterJson, "useLowGravity2", emitter.useLowGravity2);
                     readCurvePoints(emitterJson.get("durationValuesGravity"), emitter.gravityCurve);
                     emitter.highWind1 = readFloat(emitterJson, "highWind1", emitter.highWind1);
                     emitter.highWind2 = readFloat(emitterJson, "highWind2", emitter.highWind2);
                     emitter.useHighWind2 = readBool(emitterJson, "useHighWind2", emitter.useHighWind2);
                     emitter.lowWind1 = readFloat(emitterJson, "lowWind1", emitter.lowWind1);
                     emitter.lowWind2 = readFloat(emitterJson, "lowWind2", emitter.lowWind2);
                     emitter.useLowWind2 = readBool(emitterJson, "useLowWind2", emitter.useLowWind2);
                     readCurvePoints(emitterJson.get("durationValuesWind"), emitter.windCurve);
                     emitter.useLowLife2 = readBool(emitterJson, "useLowLife2", emitter.useLowLife2);
                     emitter.useHighLife2 = readBool(emitterJson, "useHighLife2", emitter.useHighLife2);
                     emitter.lowLife1 = readFloat(emitterJson, "lowLife1", emitter.lowLife1);
                     emitter.lowLife2 = readFloat(emitterJson, "lowLife2", emitter.lowLife2);
                     emitter.highLife1 = readFloat(emitterJson, "highLife1", emitter.highLife1);
                     emitter.highLife2 = readFloat(emitterJson, "highLife2", emitter.highLife2);
                     readCurvePoints(emitterJson.get("durationValuesLife"), emitter.lifeCurve);
                     emitter.splitXAndY = readBool(emitterJson, "splitXAndY", emitter.splitXAndY);
                     emitter.useLowSizeX2 = readBool(emitterJson, "useLowSizeX2", emitter.useLowSizeX2);
                     emitter.useHighSizeX2 = readBool(emitterJson, "useHighSizeX2", emitter.useHighSizeX2);
                     emitter.lowSizeX1 = readFloat(emitterJson, "lowSizeX1", emitter.lowSizeX1);
                     emitter.lowSizeX2 = readFloat(emitterJson, "lowSizeX2", emitter.lowSizeX2);
                     emitter.highSizeX1 = readFloat(emitterJson, "highSizeX1", emitter.highSizeX1);
                     emitter.highSizeX2 = readFloat(emitterJson, "highSizeX2", emitter.highSizeX2);
                     readCurvePoints(emitterJson.get("durationValuesSizeX"), emitter.sizeXCurve);
                     emitter.useLowSizeY2 = readBool(emitterJson, "useLowSizeY2", emitter.useLowSizeY2);
                     emitter.useHighSizeY2 = readBool(emitterJson, "useHighSizeY2", emitter.useHighSizeY2);
                     emitter.lowSizeY1 = readFloat(emitterJson, "lowSizeY1", emitter.lowSizeY1);
                     emitter.lowSizeY2 = readFloat(emitterJson, "lowSizeY2", emitter.lowSizeY2);
                     emitter.highSizeY1 = readFloat(emitterJson, "highSizeY1", emitter.highSizeY1);
                     emitter.highSizeY2 = readFloat(emitterJson, "highSizeY2", emitter.highSizeY2);
                     readCurvePoints(emitterJson.get("durationValuesSizeY"), emitter.sizeYCurve);
                     int spawnIndex;
                     if ((value = emitterJson.get("spawn")) != null
                        && !value.isNull()
                        && (spawnIndex = (int)value.asDouble()) >= 0
                        && spawnIndex < ParticleEmitter.SpawnShape.getValues().length) {
                        emitter.spawnShape = ParticleEmitter.SpawnShape.getValues()[spawnIndex];
                     }

                     int squareModeIndex;
                     JsonValue spawnSquareJson;
                     if ((spawnSquareJson = emitterJson.get("spawnSquare")) != null
                        && !spawnSquareJson.isNull()
                        && (squareModeIndex = (int)spawnSquareJson.asDouble()) >= 0
                        && squareModeIndex < ParticleEmitter.SquareSpawnMode.getValues().length) {
                        emitter.squareSpawnMode = ParticleEmitter.SquareSpawnMode.getValues()[squareModeIndex];
                     }

                     int ellipseModeIndex;
                     if ((value = emitterJson.get("spawnEllipse")) != null
                        && !value.isNull()
                        && (ellipseModeIndex = (int)value.asDouble()) >= 0
                        && ellipseModeIndex < ParticleEmitter.EllipseSpawnMode.getValues().length) {
                        emitter.ellipseSpawnMode = ParticleEmitter.EllipseSpawnMode.getValues()[ellipseModeIndex];
                     }

                     emitter.useLowLineWidth2 = readBool(emitterJson, "useLowLineWidth2", emitter.useLowLineWidth2);
                     emitter.useHighLineWidth2 = readBool(emitterJson, "useHighLineWidth2", emitter.useHighLineWidth2);
                     emitter.useLowLineHeight2 = readBool(emitterJson, "useLowLineHeight2", emitter.useLowLineHeight2);
                     emitter.useHighLineHeight2 = readBool(emitterJson, "useHighLineHeight2", emitter.useHighLineHeight2);
                     emitter.lowLineWidth1 = readFloat(emitterJson, "lowLineWidth1", emitter.lowLineWidth1);
                     emitter.lowLineWidth2 = readFloat(emitterJson, "lowLineWidth2", emitter.lowLineWidth2);
                     emitter.highLineWidth1 = readFloat(emitterJson, "highLineWidth1", emitter.highLineWidth1);
                     emitter.highLineWidth2 = readFloat(emitterJson, "highLineWidth2", emitter.highLineWidth2);
                     emitter.lowLineHeight1 = readFloat(emitterJson, "lowLineHeight1", emitter.lowLineHeight1);
                     emitter.lowLineHeight2 = readFloat(emitterJson, "lowLineHeight2", emitter.lowLineHeight2);
                     emitter.highLineHeight1 = readFloat(emitterJson, "highLineHeight1", emitter.highLineHeight1);
                     emitter.highLineHeight2 = readFloat(emitterJson, "highLineHeight2", emitter.highLineHeight2);
                     readCurvePoints(emitterJson.get("durationValuesLineWidth"), emitter.lineWidthCurve);
                     readCurvePoints(emitterJson.get("durationValuesLineHeight"), emitter.lineHeightCurve);
                     emitter.useLowSquareWidth2 = readBool(emitterJson, "useLowSquareWidth2", emitter.useLowSquareWidth2);
                     emitter.useHighSquareWidth2 = readBool(emitterJson, "useHighSquareWidth2", emitter.useHighSquareWidth2);
                     emitter.useLowSquareHeight2 = readBool(emitterJson, "useLowSquareHeight2", emitter.useLowSquareHeight2);
                     emitter.useHighSquareHeight2 = readBool(emitterJson, "useHighSquareHeight2", emitter.useHighSquareHeight2);
                     emitter.lowSquareWidth1 = readFloat(emitterJson, "lowSquareWidth1", emitter.lowSquareWidth1);
                     emitter.lowSquareWidth2 = readFloat(emitterJson, "lowSquareWidth2", emitter.lowSquareWidth2);
                     emitter.highSquareWidth1 = readFloat(emitterJson, "highSquareWidth1", emitter.highSquareWidth1);
                     emitter.highSquareWidth2 = readFloat(emitterJson, "highSquareWidth2", emitter.highSquareWidth2);
                     emitter.lowSquareHeight1 = readFloat(emitterJson, "lowSquareHeight1", emitter.lowSquareHeight1);
                     emitter.lowSquareHeight2 = readFloat(emitterJson, "lowSquareHeight2", emitter.lowSquareHeight2);
                     emitter.highSquareHeight1 = readFloat(emitterJson, "highSquareHeight1", emitter.highSquareHeight1);
                     emitter.highSquareHeight2 = readFloat(emitterJson, "highSquareHeight2", emitter.highSquareHeight2);
                     readCurvePoints(emitterJson.get("durationValuesSquareWidth"), emitter.squareWidthCurve);
                     readCurvePoints(emitterJson.get("durationValuesSquareHeight"), emitter.squareHeightCurve);
                     emitter.useLowEllipseWidth2 = readBool(emitterJson, "useLowEllipseWidth2", emitter.useLowEllipseWidth2);
                     emitter.useHighEllipseWidth2 = readBool(emitterJson, "useHighEllipseWidth2", emitter.useHighEllipseWidth2);
                     emitter.useLowEllipseHeight2 = readBool(emitterJson, "useLowEllipseHeight2", emitter.useLowEllipseHeight2);
                     emitter.useHighEllipseHeight2 = readBool(emitterJson, "useHighEllipseHeight2", emitter.useHighEllipseHeight2);
                     emitter.lowEllipseWidth1 = readFloat(emitterJson, "lowEllipseWidth1", emitter.lowEllipseWidth1);
                     emitter.lowEllipseWidth2 = readFloat(emitterJson, "lowEllipseWidth2", emitter.lowEllipseWidth2);
                     emitter.highEllipseWidth1 = readFloat(emitterJson, "highEllipseWidth1", emitter.highEllipseWidth1);
                     emitter.highEllipseWidth2 = readFloat(emitterJson, "highEllipseWidth2", emitter.highEllipseWidth2);
                     emitter.lowEllipseHeight1 = readFloat(emitterJson, "lowEllipseHeight1", emitter.lowEllipseHeight1);
                     emitter.lowEllipseHeight2 = readFloat(emitterJson, "lowEllipseHeight2", emitter.lowEllipseHeight2);
                     emitter.highEllipseHeight1 = readFloat(emitterJson, "highEllipseHeight1", emitter.highEllipseHeight1);
                     emitter.highEllipseHeight2 = readFloat(emitterJson, "highEllipseHeight2", emitter.highEllipseHeight2);
                     readCurvePoints(emitterJson.get("durationValuesEllipseWidth"), emitter.ellipseWidthCurve);
                     readCurvePoints(emitterJson.get("durationValuesEllipseHeight"), emitter.ellipseHeightCurve);
                     readCurvePoints(emitterJson.get("durationValuesTransparency"), emitter.transparencyCurve);
                     if ((value = emitterJson.get("tintPoints")) != null && value.isArray()) {
                        emitter.tintPoints.clear();
                        JsonValue.JsonIterator tintIter = value.iterator();

                        while (tintIter.hasNext()) {
                           if ((value = (JsonValue)tintIter.next()).isArray() && value.size >= 5) {
                              float tintPosition = value.get(0).asFloat();
                              float r = value.get(1).asFloat();
                              float g = value.get(2).asFloat();
                              float b = value.get(3).asFloat();
                              float a = value.get(4).asFloat();
                              Color tintColor = new Color(r, g, b, a);
                              emitter.tintPoints.add(new TintPoint(tintPosition, tintColor, true));
                           }
                        }
                     }

                     this.emitters.add(emitter);
                  }
               }
            }
         }
      }
   }

}
