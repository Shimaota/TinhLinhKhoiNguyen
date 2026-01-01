package com.tlnt.engine.particle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.util.GameUtils;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.particle.BezierUtil;
import com.tlnt.engine.particle.CurvePoint;
import com.tlnt.engine.particle.Particle;
import com.tlnt.engine.particle.ParticleEffect;
import com.tlnt.engine.particle.ParticlePool;
import com.tlnt.engine.particle.TintPoint;
import com.tlnt.engine.render.TextureAsset;

public final class ParticleEmitter {
   public Pool<Particle> particlePool = new ParticlePool(this);
   public ParticleEffect effect;
   public Array<Particle> particles = new Array<>();
   public boolean active;
   public String name;
   public int spriteMode;
   public Array<String> imagePaths;
   public int selectedImageIndex;
   public int animationFps;
   public float durationTimer;
   public float durationPercent;
   public boolean useHighDuration;
   public float lowDuration;
   public float highDuration;
   public float currentDuration;
   public boolean useHighDelay;
   public float lowDelay;
   public float highDelay;
   public float delayTimer;
   public boolean attached;
   public boolean premultipliedAlpha;
   public boolean additive;
   public boolean continuous;
   public boolean useCurves;
   public boolean useCurvesEmission;
   public boolean useCurvesLife;
   public boolean useCurvesRotation;
   public boolean useCurvesVelocity;
   public boolean useCurvesAngle;
   public boolean useCurvesGravity;
   public boolean useCurvesWind;
   public boolean useCurvesSizeX;
   public boolean useCurvesSizeY;
   public boolean useCurvesTrailMaxPoints;
   public boolean useCurvesTrailStepTime;
   public boolean useCurvesTrailAlpha;
   public boolean useCurvesBloomIntensity;
   public boolean useCurvesBloomSizeMultiplier;
   public boolean useCurvesLineWidth;
   public boolean useCurvesLineHeight;
   public boolean useCurvesSquareWidth;
   public boolean useCurvesSquareHeight;
   public boolean useCurvesTransparency;
   public int minParticleCount;
   public int maxParticleCount;
   public float emissionTimer;
   public float emissionInterval;
   public boolean useLowEmission2;
   public boolean useHighEmission2;
   public int lowEmission1;
   public int lowEmission2;
   public int highEmission1;
   public int highEmission2;
   public Array<CurvePoint> emissionCurve;
   public boolean useHighRotation2;
   public boolean useLowRotation2;
   public float highRotation1;
   public float highRotation2;
   public float lowRotation1;
   public float lowRotation2;
   public Array<CurvePoint> rotationCurve;
   public boolean aligned;
   public float highVelocity1;
   public float highVelocity2;
   public boolean useHighVelocity2;
   public float lowVelocity1;
   public float lowVelocity2;
   public boolean useLowVelocity2;
   public Array<CurvePoint> velocityCurve;
   public float highAngle1;
   public float highAngle2;
   public boolean useHighAngle2;
   public float lowAngle1;
   public float lowAngle2;
   public boolean useLowAngle2;
   public Array<CurvePoint> angleCurve;
   public float highGravity1;
   public float highGravity2;
   public boolean useHighGravity2;
   public float lowGravity1;
   public float lowGravity2;
   public boolean useLowGravity2;
   public Array<CurvePoint> gravityCurve;
   public float highWind1;
   public float highWind2;
   public boolean useHighWind2;
   public float lowWind1;
   public float lowWind2;
   public boolean useLowWind2;
   public Array<CurvePoint> windCurve;
   public boolean useLowLife2;
   public boolean useHighLife2;
   public float lowLife1;
   public float lowLife2;
   public float highLife1;
   public float highLife2;
   public Array<CurvePoint> lifeCurve;
   public boolean splitXAndY;
   public boolean useLowSizeX2;
   public boolean useHighSizeX2;
   public float lowSizeX1;
   public float lowSizeX2;
   public float highSizeX1;
   public float highSizeX2;
   public Array<CurvePoint> sizeXCurve;
   public boolean useLowSizeY2;
   public boolean useHighSizeY2;
   public float lowSizeY1;
   public float lowSizeY2;
   public float highSizeY1;
   public float highSizeY2;
   public Array<CurvePoint> sizeYCurve;
   public int spawnShape;
   public boolean useLowLineWidth2;
   public boolean useHighLineWidth2;
   public boolean useLowLineHeight2;
   public boolean useHighLineHeight2;
   public float lowLineWidth1;
   public float lowLineWidth2;
   public float highLineWidth1;
   public float highLineWidth2;
   public float lowLineHeight1;
   public float lowLineHeight2;
   public float highLineHeight1;
   public float highLineHeight2;
   public Array<CurvePoint> lineWidthCurve;
   public Array<CurvePoint> lineHeightCurve;
   public int squareSpawnMode;
   public boolean useLowSquareWidth2;
   public boolean useHighSquareWidth2;
   public boolean useLowSquareHeight2;
   public boolean useHighSquareHeight2;
   public float lowSquareWidth1;
   public float lowSquareWidth2;
   public float highSquareWidth1;
   public float highSquareWidth2;
   public float lowSquareHeight1;
   public float lowSquareHeight2;
   public float highSquareHeight1;
   public float highSquareHeight2;
   public Array<CurvePoint> squareWidthCurve;
   public Array<CurvePoint> squareHeightCurve;
   public int ellipseSpawnMode;
   public boolean useLowEllipseWidth2;
   public boolean useHighEllipseWidth2;
   public boolean useLowEllipseHeight2;
   public boolean useHighEllipseHeight2;
   public float lowEllipseWidth1;
   public float lowEllipseWidth2;
   public float highEllipseWidth1;
   public float highEllipseWidth2;
   public float lowEllipseHeight1;
   public float lowEllipseHeight2;
   public float highEllipseHeight1;
   public float highEllipseHeight2;
   public Array<CurvePoint> ellipseWidthCurve;
   public Array<CurvePoint> ellipseHeightCurve;
   public Array<CurvePoint> transparencyCurve;
   public Array<TintPoint> tintPoints;
   public boolean trailEnabled;
   public boolean useLowTrailMaxPoints2;
   public boolean useHighTrailMaxPoints2;
   public int lowTrailMaxPoints1;
   public int lowTrailMaxPoints2;
   public int highTrailMaxPoints1;
   public int highTrailMaxPoints2;
   public Array<CurvePoint> trailMaxPointsCurve;
   public boolean useLowTrailStepTime2;
   public boolean useHighTrailStepTime2;
   public float lowTrailStepTime1;
   public float lowTrailStepTime2;
   public float highTrailStepTime1;
   public float highTrailStepTime2;
   public Array<CurvePoint> trailStepTimeCurve;
   public boolean useLowTrailAlpha2;
   public boolean useHighTrailAlpha2;
   public float lowTrailAlpha1;
   public float lowTrailAlpha2;
   public float highTrailAlpha1;
   public float highTrailAlpha2;
   public Array<CurvePoint> trailAlphaCurve;
   public boolean bloomEnabled;
   public boolean useLowBloomIntensity2;
   public boolean useHighBloomIntensity2;
   public float lowBloomIntensity1;
   public float lowBloomIntensity2;
   public float highBloomIntensity1;
   public float highBloomIntensity2;
   public Array<CurvePoint> bloomIntensityCurve;
   public boolean useLowBloomSizeMultiplier2;
   public boolean useHighBloomSizeMultiplier2;
   public float lowBloomSizeMultiplier1;
   public float lowBloomSizeMultiplier2;
   public float highBloomSizeMultiplier1;
   public float highBloomSizeMultiplier2;
   public Array<CurvePoint> bloomSizeMultiplierCurve;

   public ParticleEmitter(ParticleEffect effect) {
      this.active = true;
      this.name = "Emitter";
      this.spriteMode = ParticleEmitter.SpriteMode.SINGLE;
      this.imagePaths = new Array<>();
      this.selectedImageIndex = 0;
      this.animationFps = 12;
      this.lowDuration = 2.0F;
      this.lowDelay = 0.0F;
      this.continuous = true;
      this.useCurves = false;
      this.useCurvesEmission = false;
      this.useCurvesLife = false;
      this.useCurvesRotation = false;
      this.useCurvesVelocity = false;
      this.useCurvesAngle = false;
      this.useCurvesGravity = false;
      this.useCurvesWind = false;
      this.useCurvesSizeX = false;
      this.useCurvesSizeY = false;
      this.useCurvesTrailMaxPoints = false;
      this.useCurvesTrailStepTime = false;
      this.useCurvesTrailAlpha = false;
      this.useCurvesBloomIntensity = false;
      this.useCurvesBloomSizeMultiplier = false;
      this.useCurvesLineWidth = false;
      this.useCurvesLineHeight = false;
      this.useCurvesSquareWidth = false;
      this.useCurvesSquareHeight = false;
      this.useCurvesTransparency = false;
      this.maxParticleCount = 200;
      this.highEmission1 = 40;
      this.emissionCurve = new Array<>();
      this.rotationCurve = new Array<>();
      this.aligned = false;
      this.highVelocity1 = 0.0F;
      this.highVelocity2 = 0.0F;
      this.useHighVelocity2 = false;
      this.lowVelocity1 = 0.0F;
      this.lowVelocity2 = 0.0F;
      this.useLowVelocity2 = false;
      this.velocityCurve = new Array<>();
      this.highAngle1 = 0.0F;
      this.highAngle2 = 0.0F;
      this.useHighAngle2 = false;
      this.lowAngle1 = 0.0F;
      this.lowAngle2 = 0.0F;
      this.useLowAngle2 = false;
      this.angleCurve = new Array<>();
      this.highGravity1 = 0.0F;
      this.highGravity2 = 0.0F;
      this.useHighGravity2 = false;
      this.lowGravity1 = 0.0F;
      this.lowGravity2 = 0.0F;
      this.useLowGravity2 = false;
      this.gravityCurve = new Array<>();
      this.highWind1 = 0.0F;
      this.highWind2 = 0.0F;
      this.useHighWind2 = false;
      this.lowWind1 = 0.0F;
      this.lowWind2 = 0.0F;
      this.useLowWind2 = false;
      this.windCurve = new Array<>();
      this.highLife1 = 0.5F;
      this.lifeCurve = new Array<>();
      this.highSizeX1 = 50.0F;
      this.sizeXCurve = new Array<>();
      this.highSizeY1 = 50.0F;
      this.sizeYCurve = new Array<>();
      this.spawnShape = ParticleEmitter.SpawnShape.POINT;
      this.lineWidthCurve = new Array<>();
      this.lineHeightCurve = new Array<>();
      this.squareSpawnMode = ParticleEmitter.SquareSpawnMode.EDGE;
      this.squareWidthCurve = new Array<>();
      this.squareHeightCurve = new Array<>();
      this.ellipseSpawnMode = ParticleEmitter.EllipseSpawnMode.EDGE;
      this.ellipseWidthCurve = new Array<>();
      this.ellipseHeightCurve = new Array<>();
      this.transparencyCurve = new Array<>();
      this.tintPoints = new Array<>();
      this.trailEnabled = false;
      this.lowTrailMaxPoints1 = 12;
      this.highTrailMaxPoints1 = 12;
      this.trailMaxPointsCurve = new Array<>();
      this.lowTrailStepTime1 = 0.02F;
      this.highTrailStepTime1 = 0.02F;
      this.trailStepTimeCurve = new Array<>();
      this.lowTrailAlpha1 = 0.6F;
      this.highTrailAlpha1 = 0.6F;
      this.trailAlphaCurve = new Array<>();
      this.bloomEnabled = false;
      this.lowBloomIntensity1 = 0.5F;
      this.highBloomIntensity1 = 0.5F;
      this.bloomIntensityCurve = new Array<>();
      this.lowBloomSizeMultiplier1 = 1.5F;
      this.highBloomSizeMultiplier1 = 1.5F;
      this.bloomSizeMultiplierCurve = new Array<>();
      this.effect = effect;
      this.emissionCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.lifeCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.sizeXCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.sizeYCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.squareWidthCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.squareHeightCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.ellipseWidthCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.ellipseHeightCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.lineWidthCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.lineHeightCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.rotationCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.velocityCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.angleCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.gravityCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.windCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.transparencyCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.trailMaxPointsCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.trailStepTimeCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.trailAlphaCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.bloomIntensityCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.bloomSizeMultiplierCurve.add(new CurvePoint(0.0F, 1.0F, false));
      this.tintPoints.add(new TintPoint(0.0F, new Color(1.0F, 1.0F, 1.0F, 1.0F), false));
   }

   public ParticleEmitter(ParticleEffect effect, ParticleEmitter source) {
      this.active = true;
      this.name = "Emitter";
      this.spriteMode = ParticleEmitter.SpriteMode.SINGLE;
      this.imagePaths = new Array<>();
      this.selectedImageIndex = 0;
      this.animationFps = 12;
      this.lowDuration = 2.0F;
      this.lowDelay = 0.0F;
      this.continuous = true;
      this.useCurves = false;
      this.useCurvesEmission = false;
      this.useCurvesLife = false;
      this.useCurvesRotation = false;
      this.useCurvesVelocity = false;
      this.useCurvesAngle = false;
      this.useCurvesGravity = false;
      this.useCurvesWind = false;
      this.useCurvesSizeX = false;
      this.useCurvesSizeY = false;
      this.useCurvesTrailMaxPoints = false;
      this.useCurvesTrailStepTime = false;
      this.useCurvesTrailAlpha = false;
      this.useCurvesBloomIntensity = false;
      this.useCurvesBloomSizeMultiplier = false;
      this.useCurvesLineWidth = false;
      this.useCurvesLineHeight = false;
      this.useCurvesSquareWidth = false;
      this.useCurvesSquareHeight = false;
      this.useCurvesTransparency = false;
      this.maxParticleCount = 200;
      this.highEmission1 = 40;
      this.emissionCurve = new Array<>();
      this.rotationCurve = new Array<>();
      this.aligned = false;
      this.highVelocity1 = 0.0F;
      this.highVelocity2 = 0.0F;
      this.useHighVelocity2 = false;
      this.lowVelocity1 = 0.0F;
      this.lowVelocity2 = 0.0F;
      this.useLowVelocity2 = false;
      this.velocityCurve = new Array<>();
      this.highAngle1 = 0.0F;
      this.highAngle2 = 0.0F;
      this.useHighAngle2 = false;
      this.lowAngle1 = 0.0F;
      this.lowAngle2 = 0.0F;
      this.useLowAngle2 = false;
      this.angleCurve = new Array<>();
      this.highGravity1 = 0.0F;
      this.highGravity2 = 0.0F;
      this.useHighGravity2 = false;
      this.lowGravity1 = 0.0F;
      this.lowGravity2 = 0.0F;
      this.useLowGravity2 = false;
      this.gravityCurve = new Array<>();
      this.highWind1 = 0.0F;
      this.highWind2 = 0.0F;
      this.useHighWind2 = false;
      this.lowWind1 = 0.0F;
      this.lowWind2 = 0.0F;
      this.useLowWind2 = false;
      this.windCurve = new Array<>();
      this.highLife1 = 0.5F;
      this.lifeCurve = new Array<>();
      this.highSizeX1 = 50.0F;
      this.sizeXCurve = new Array<>();
      this.highSizeY1 = 50.0F;
      this.sizeYCurve = new Array<>();
      this.spawnShape = ParticleEmitter.SpawnShape.POINT;
      this.lineWidthCurve = new Array<>();
      this.lineHeightCurve = new Array<>();
      this.squareSpawnMode = ParticleEmitter.SquareSpawnMode.EDGE;
      this.squareWidthCurve = new Array<>();
      this.squareHeightCurve = new Array<>();
      this.ellipseSpawnMode = ParticleEmitter.EllipseSpawnMode.EDGE;
      this.ellipseWidthCurve = new Array<>();
      this.ellipseHeightCurve = new Array<>();
      this.transparencyCurve = new Array<>();
      this.tintPoints = new Array<>();
      this.trailEnabled = false;
      this.lowTrailMaxPoints1 = 12;
      this.highTrailMaxPoints1 = 12;
      this.trailMaxPointsCurve = new Array<>();
      this.lowTrailStepTime1 = 0.02F;
      this.highTrailStepTime1 = 0.02F;
      this.trailStepTimeCurve = new Array<>();
      this.lowTrailAlpha1 = 0.6F;
      this.highTrailAlpha1 = 0.6F;
      this.trailAlphaCurve = new Array<>();
      this.bloomEnabled = false;
      this.lowBloomIntensity1 = 0.5F;
      this.highBloomIntensity1 = 0.5F;
      this.bloomIntensityCurve = new Array<>();
      this.lowBloomSizeMultiplier1 = 1.5F;
      this.highBloomSizeMultiplier1 = 1.5F;
      this.bloomSizeMultiplierCurve = new Array<>();
      this.effect = effect;
      this.name = source.name;
      this.active = source.active;
      this.spriteMode = source.spriteMode;
      this.imagePaths.addAll(source.imagePaths);
      this.selectedImageIndex = source.selectedImageIndex;
      this.animationFps = source.animationFps;
      this.useHighDuration = source.useHighDuration;
      this.lowDuration = source.lowDuration;
      this.highDuration = source.highDuration;
      this.useHighDelay = source.useHighDelay;
      this.lowDelay = source.lowDelay;
      this.highDelay = source.highDelay;
      this.attached = source.attached;
      this.premultipliedAlpha = source.premultipliedAlpha;
      this.additive = source.additive;
      this.continuous = source.continuous;
      this.useCurves = source.useCurves;
      this.useCurvesEmission = source.useCurvesEmission;
      this.useCurvesLife = source.useCurvesLife;
      this.useCurvesRotation = source.useCurvesRotation;
      this.useCurvesVelocity = source.useCurvesVelocity;
      this.useCurvesAngle = source.useCurvesAngle;
      this.useCurvesGravity = source.useCurvesGravity;
      this.useCurvesWind = source.useCurvesWind;
      this.useCurvesSizeX = source.useCurvesSizeX;
      this.useCurvesSizeY = source.useCurvesSizeY;
      this.useCurvesTrailMaxPoints = source.useCurvesTrailMaxPoints;
      this.useCurvesTrailStepTime = source.useCurvesTrailStepTime;
      this.useCurvesTrailAlpha = source.useCurvesTrailAlpha;
      this.useCurvesBloomIntensity = source.useCurvesBloomIntensity;
      this.useCurvesBloomSizeMultiplier = source.useCurvesBloomSizeMultiplier;
      this.useCurvesLineWidth = source.useCurvesLineWidth;
      this.useCurvesLineHeight = source.useCurvesLineHeight;
      this.useCurvesSquareWidth = source.useCurvesSquareWidth;
      this.useCurvesSquareHeight = source.useCurvesSquareHeight;
      this.useCurvesTransparency = source.useCurvesTransparency;
      this.trailEnabled = source.trailEnabled;
      this.useLowTrailMaxPoints2 = source.useLowTrailMaxPoints2;
      this.useHighTrailMaxPoints2 = source.useHighTrailMaxPoints2;
      this.lowTrailMaxPoints1 = source.lowTrailMaxPoints1;
      this.lowTrailMaxPoints2 = source.lowTrailMaxPoints2;
      this.highTrailMaxPoints1 = source.highTrailMaxPoints1;
      this.highTrailMaxPoints2 = source.highTrailMaxPoints2;
      CurvePoint.copyPoints(source.trailMaxPointsCurve, this.trailMaxPointsCurve);
      this.useLowTrailStepTime2 = source.useLowTrailStepTime2;
      this.useHighTrailStepTime2 = source.useHighTrailStepTime2;
      this.lowTrailStepTime1 = source.lowTrailStepTime1;
      this.lowTrailStepTime2 = source.lowTrailStepTime2;
      this.highTrailStepTime1 = source.highTrailStepTime1;
      this.highTrailStepTime2 = source.highTrailStepTime2;
      CurvePoint.copyPoints(source.trailStepTimeCurve, this.trailStepTimeCurve);
      this.useLowTrailAlpha2 = source.useLowTrailAlpha2;
      this.useHighTrailAlpha2 = source.useHighTrailAlpha2;
      this.lowTrailAlpha1 = source.lowTrailAlpha1;
      this.lowTrailAlpha2 = source.lowTrailAlpha2;
      this.highTrailAlpha1 = source.highTrailAlpha1;
      this.highTrailAlpha2 = source.highTrailAlpha2;
      CurvePoint.copyPoints(source.trailAlphaCurve, this.trailAlphaCurve);
      this.bloomEnabled = source.bloomEnabled;
      this.useLowBloomIntensity2 = source.useLowBloomIntensity2;
      this.useHighBloomIntensity2 = source.useHighBloomIntensity2;
      this.lowBloomIntensity1 = source.lowBloomIntensity1;
      this.lowBloomIntensity2 = source.lowBloomIntensity2;
      this.highBloomIntensity1 = source.highBloomIntensity1;
      this.highBloomIntensity2 = source.highBloomIntensity2;
      CurvePoint.copyPoints(source.bloomIntensityCurve, this.bloomIntensityCurve);
      this.useLowBloomSizeMultiplier2 = source.useLowBloomSizeMultiplier2;
      this.useHighBloomSizeMultiplier2 = source.useHighBloomSizeMultiplier2;
      this.lowBloomSizeMultiplier1 = source.lowBloomSizeMultiplier1;
      this.lowBloomSizeMultiplier2 = source.lowBloomSizeMultiplier2;
      this.highBloomSizeMultiplier1 = source.highBloomSizeMultiplier1;
      this.highBloomSizeMultiplier2 = source.highBloomSizeMultiplier2;
      CurvePoint.copyPoints(source.bloomSizeMultiplierCurve, this.bloomSizeMultiplierCurve);
      this.minParticleCount = source.minParticleCount;
      this.maxParticleCount = source.maxParticleCount;
      this.useLowEmission2 = source.useLowEmission2;
      this.useHighEmission2 = source.useHighEmission2;
      this.lowEmission1 = source.lowEmission1;
      this.lowEmission2 = source.lowEmission2;
      this.highEmission1 = source.highEmission1;
      this.highEmission2 = source.highEmission2;
      CurvePoint.copyPoints(source.emissionCurve, this.emissionCurve);
      this.useHighRotation2 = source.useHighRotation2;
      this.useLowRotation2 = source.useLowRotation2;
      this.highRotation1 = source.highRotation1;
      this.highRotation2 = source.highRotation2;
      this.lowRotation1 = source.lowRotation1;
      this.lowRotation2 = source.lowRotation2;
      this.aligned = source.aligned;
      CurvePoint.copyPoints(source.rotationCurve, this.rotationCurve);
      this.highVelocity1 = source.highVelocity1;
      this.highVelocity2 = source.highVelocity2;
      this.useHighVelocity2 = source.useHighVelocity2;
      this.lowVelocity1 = source.lowVelocity1;
      this.lowVelocity2 = source.lowVelocity2;
      this.useLowVelocity2 = source.useLowVelocity2;
      CurvePoint.copyPoints(source.velocityCurve, this.velocityCurve);
      this.highAngle1 = source.highAngle1;
      this.highAngle2 = source.highAngle2;
      this.useHighAngle2 = source.useHighAngle2;
      this.lowAngle1 = source.lowAngle1;
      this.lowAngle2 = source.lowAngle2;
      this.useLowAngle2 = source.useLowAngle2;
      CurvePoint.copyPoints(source.angleCurve, this.angleCurve);
      this.highGravity1 = source.highGravity1;
      this.highGravity2 = source.highGravity2;
      this.useHighGravity2 = source.useHighGravity2;
      this.lowGravity1 = source.lowGravity1;
      this.lowGravity2 = source.lowGravity2;
      this.useLowGravity2 = source.useLowGravity2;
      CurvePoint.copyPoints(source.gravityCurve, this.gravityCurve);
      this.highWind1 = source.highWind1;
      this.highWind2 = source.highWind2;
      this.useHighWind2 = source.useHighWind2;
      this.lowWind1 = source.lowWind1;
      this.lowWind2 = source.lowWind2;
      this.useLowWind2 = source.useLowWind2;
      CurvePoint.copyPoints(source.windCurve, this.windCurve);
      this.useLowLife2 = source.useLowLife2;
      this.useHighLife2 = source.useHighLife2;
      this.lowLife1 = source.lowLife1;
      this.lowLife2 = source.lowLife2;
      this.highLife1 = source.highLife1;
      this.highLife2 = source.highLife2;
      CurvePoint.copyPoints(source.lifeCurve, this.lifeCurve);
      this.splitXAndY = source.splitXAndY;
      this.useLowSizeX2 = source.useLowSizeX2;
      this.useHighSizeX2 = source.useHighSizeX2;
      this.lowSizeX1 = source.lowSizeX1;
      this.lowSizeX2 = source.lowSizeX2;
      this.highSizeX1 = source.highSizeX1;
      this.highSizeX2 = source.highSizeX2;
      CurvePoint.copyPoints(source.sizeXCurve, this.sizeXCurve);
      this.useLowSizeY2 = source.useLowSizeY2;
      this.useHighSizeY2 = source.useHighSizeY2;
      this.lowSizeY1 = source.lowSizeY1;
      this.lowSizeY2 = source.lowSizeY2;
      this.highSizeY1 = source.highSizeY1;
      this.highSizeY2 = source.highSizeY2;
      CurvePoint.copyPoints(source.sizeYCurve, this.sizeYCurve);
      this.spawnShape = source.spawnShape;
      this.useLowLineWidth2 = source.useLowLineWidth2;
      this.useHighLineWidth2 = source.useHighLineWidth2;
      this.useLowLineHeight2 = source.useLowLineHeight2;
      this.useHighLineHeight2 = source.useHighLineHeight2;
      this.lowLineWidth1 = source.lowLineWidth1;
      this.lowLineWidth2 = source.lowLineWidth2;
      this.highLineWidth1 = source.highLineWidth1;
      this.highLineWidth2 = source.highLineWidth2;
      this.lowLineHeight1 = source.lowLineHeight1;
      this.lowLineHeight2 = source.lowLineHeight2;
      this.highLineHeight1 = source.highLineHeight1;
      this.highLineHeight2 = source.highLineHeight2;
      CurvePoint.copyPoints(source.lineWidthCurve, this.lineWidthCurve);
      CurvePoint.copyPoints(source.lineHeightCurve, this.lineHeightCurve);
      this.squareSpawnMode = source.squareSpawnMode;
      this.useLowSquareWidth2 = source.useLowSquareWidth2;
      this.useHighSquareWidth2 = source.useHighSquareWidth2;
      this.useLowSquareHeight2 = source.useLowSquareHeight2;
      this.useHighSquareHeight2 = source.useHighSquareHeight2;
      this.lowSquareWidth1 = source.lowSquareWidth1;
      this.lowSquareWidth2 = source.lowSquareWidth2;
      this.highSquareWidth1 = source.highSquareWidth1;
      this.highSquareWidth2 = source.highSquareWidth2;
      this.lowSquareHeight1 = source.lowSquareHeight1;
      this.lowSquareHeight2 = source.lowSquareHeight2;
      this.highSquareHeight1 = source.highSquareHeight1;
      this.highSquareHeight2 = source.highSquareHeight2;
      CurvePoint.copyPoints(source.squareWidthCurve, this.squareWidthCurve);
      CurvePoint.copyPoints(source.squareHeightCurve, this.squareHeightCurve);
      this.ellipseSpawnMode = source.ellipseSpawnMode;
      this.useLowEllipseWidth2 = source.useLowEllipseWidth2;
      this.useHighEllipseWidth2 = source.useHighEllipseWidth2;
      this.useLowEllipseHeight2 = source.useLowEllipseHeight2;
      this.useHighEllipseHeight2 = source.useHighEllipseHeight2;
      this.lowEllipseWidth1 = source.lowEllipseWidth1;
      this.lowEllipseWidth2 = source.lowEllipseWidth2;
      this.highEllipseWidth1 = source.highEllipseWidth1;
      this.highEllipseWidth2 = source.highEllipseWidth2;
      this.lowEllipseHeight1 = source.lowEllipseHeight1;
      this.lowEllipseHeight2 = source.lowEllipseHeight2;
      this.highEllipseHeight1 = source.highEllipseHeight1;
      CurvePoint.copyPoints(source.ellipseWidthCurve, this.ellipseWidthCurve);
      CurvePoint.copyPoints(source.ellipseHeightCurve, this.ellipseHeightCurve);
      CurvePoint.copyPoints(source.transparencyCurve, this.transparencyCurve);
      Array targetTints = this.tintPoints;
      Array sourceTints = source.tintPoints;
      if (source.tintPoints != null && targetTints != null) {
         int sourceCount = sourceTints.size;
         int targetCount = targetTints.size;
         if (targetTints.size < sourceCount) {
            for (int i = targetCount; i < sourceCount; i++) {
               targetTints.add(new TintPoint(0.0F, new Color(), true));
            }
         }

         for (int i = 0; i < sourceCount; i++) {
            TintPoint src = (TintPoint)sourceTints.get(i);
            TintPoint dst;
            (dst = (TintPoint)targetTints.get(i)).position = src.position;
            dst.color.set(src.color);
            dst.deletable = ((i != 0 ? 1 : 0) != 0);
         }

         if (targetCount > sourceCount) {
            targetTints.truncate(sourceCount);
         }
      }
   }

   public final void update(float delta) {
      float dt = delta;
      ParticleEmitter emitter = this;

      for (int i = this.particles.size - 1; i >= 0; i--) {
         Particle particle;
         (particle = emitter.particles.get(i)).update(dt);
         int deadFlag;
         if (particle.age >= particle.lifeDuration) {
            deadFlag = 1;
         } else {
            deadFlag = 0;
         }

         if (deadFlag != 0) {
            emitter.particlePool.free(particle);
            emitter.particles.removeIndex(i);
         }
      }

      if (this.active) {
         if (this.delayTimer > 0.0F) {
            this.delayTimer -= delta;
            if (this.delayTimer > 0.0F) {
               return;
            }
         }

         this.durationTimer += delta;
         this.durationPercent = Math.min(1.0F, this.durationTimer / this.currentDuration);
         if (this.durationPercent >= 1.0F) {
            if (!this.continuous) {
               return;
            }

            this.durationTimer = this.durationTimer - this.currentDuration;
            this.currentDuration = this.useHighDuration ? GameUtils.randomFloat(this.lowDuration, this.highDuration) : this.lowDuration;
            this.delayTimer = this.useHighDelay ? GameUtils.randomFloat(this.lowDelay, this.highDelay) : this.lowDelay;
         }

         int lowEmission = this.useLowEmission2 ? GameUtils.randomInt(this.lowEmission1, this.lowEmission2) : this.lowEmission1;
         int highEmission = this.useHighEmission2 ? GameUtils.randomInt(this.highEmission1, this.highEmission2) : this.highEmission1;
         this.emissionInterval = 1.0F / BezierUtil.evalCurve(this.durationPercent, this.emissionCurve, (float)lowEmission, (float)highEmission);
         this.emissionTimer += delta;
         if (this.emissionTimer >= this.emissionInterval) {
            this.emissionTimer = this.emissionTimer - this.emissionInterval;
            if (this.particles.size < this.maxParticleCount) {
               this.spawnParticles(1);
            }
         }

         if (this.particles.size < this.minParticleCount) {
            this.spawnParticles(this.minParticleCount - this.particles.size);
         }
      }
   }

   public void spawnParticles(int count) {
       if (this.imagePaths.isEmpty()) {
           return;
       }
       if (this.spriteMode != SpriteMode.ANIMATED && this.selectedImageIndex == -1) {
           return;
       }
       Vector2 position = this.effect.getPosition();
       float scale = this.effect.getScale();
       for (int i = 0; i < count; ++i) {
           float aux;
           float width;
           float high;
           float low;
           Particle particle = this.particlePool.obtain();
           switch (this.spriteMode) {
               case SpriteMode.SINGLE: {
                   particle.texture = TextureAsset.obtain(this.imagePaths.get(this.selectedImageIndex));
                   break;
               }
               case SpriteMode.RANDOM: {
                   particle.texture = TextureAsset.obtain(this.imagePaths.get(GameUtils.randomInt(0, this.imagePaths.size - 1)));
                   break;
               }
               case SpriteMode.ANIMATED: {
                   particle.texture = TextureAsset.obtain(this.imagePaths.get(0));
               }
           }
           block5 : switch (this.spawnShape) {
               case SpawnShape.POINT: {
                   particle.position.set(position);
                   break;
               }
               case SpawnShape.LINE: {
                   low = this.useLowLineWidth2 ? GameUtils.randomFloat(this.lowLineWidth1, this.lowLineWidth2) : this.lowLineWidth1;
                   high = this.useHighLineWidth2 ? GameUtils.randomFloat(this.highLineWidth1, this.highLineWidth2) : this.highLineWidth1;
                   width = BezierUtil.evalCurve(this.durationPercent, this.lineWidthCurve, low, high) * scale;
                   aux = this.useLowLineHeight2 ? GameUtils.randomFloat(this.lowLineHeight1, this.lowLineHeight2) : this.lowLineHeight1;
                   low = this.useHighLineHeight2 ? GameUtils.randomFloat(this.highLineHeight1, this.highLineHeight2) : this.highLineHeight1;
                   low = BezierUtil.evalCurve(this.durationPercent, this.lineHeightCurve, aux, low) * scale;
                   high = GameUtils.randomFloat(-width * 0.5f, width * 0.5f);
                   low = GameUtils.randomFloat(-low * 0.5f, low * 0.5f);
                   particle.position.set(position).add(high, low);
                   break;
               }
               case SpawnShape.SQUARE: {
                   float highWidth;
                   float lowWidth;
                   switch (this.squareSpawnMode) {
                       case SquareSpawnMode.FILL: {
                           low = this.useLowSquareWidth2 ? GameUtils.randomFloat(this.lowSquareWidth1, this.lowSquareWidth2) : this.lowSquareWidth1;
                           high = this.useHighSquareWidth2 ? GameUtils.randomFloat(this.highSquareWidth1, this.highSquareWidth2) : this.highSquareWidth1;
                           width = BezierUtil.evalCurve(this.durationPercent, this.squareWidthCurve, low, high) * scale;
                           aux = this.useLowSquareHeight2 ? GameUtils.randomFloat(this.lowSquareHeight1, this.lowSquareHeight2) : this.lowSquareHeight1;
                           low = this.useHighSquareHeight2 ? GameUtils.randomFloat(this.highSquareHeight1, this.highSquareHeight2) : this.highSquareHeight1;
                           low = BezierUtil.evalCurve(this.durationPercent, this.squareHeightCurve, aux, low) * scale;
                           high = GameUtils.randomFloat(-width * 0.5f, width * 0.5f);
                           low = GameUtils.randomFloat(-low * 0.5f, low * 0.5f);
                           particle.position.set(position).add(high, low);
                           break;
                       }
                       case SquareSpawnMode.EDGE: {
                           float unusedA;
                           lowWidth = this.useLowSquareWidth2 ? GameUtils.randomFloat(this.lowSquareWidth1, this.lowSquareWidth2) : this.lowSquareWidth1;
                           highWidth = this.useHighSquareWidth2 ? GameUtils.randomFloat(this.highSquareWidth1, this.highSquareWidth2) : this.highSquareWidth1;
                           width = BezierUtil.evalCurve(this.durationPercent, this.squareWidthCurve, lowWidth, highWidth) * scale;
                           high = this.useLowSquareHeight2 ? GameUtils.randomFloat(this.lowSquareHeight1, this.lowSquareHeight2) : this.lowSquareHeight1;
                           low = this.useHighSquareHeight2 ? GameUtils.randomFloat(this.highSquareHeight1, this.highSquareHeight2) : this.highSquareHeight1;
                           low = BezierUtil.evalCurve(this.durationPercent, this.squareHeightCurve, high, low) * scale;
                           high = width * 0.5f;
                           aux = low * 0.5f;
                           if (width == 0.0f && low == 0.0f) {
                               particle.position.set(position);
                               break;
                           }
                           if (width == 0.0f) {
                               high = GameUtils.randomFloat(-aux, aux);
                               particle.position.set(position).add(0.0f, high);
                               break;
                           }
                           if (low == 0.0f) {
                               low = GameUtils.randomFloat(-high, high);
                               particle.position.set(position).add(low, 0.0f);
                               break;
                           }
                           float perimeterPos = 2.0f * (width + low);
                           perimeterPos = GameUtils.randomFloat(0.0f, perimeterPos);
                           if (perimeterPos <= width) {
                               low = -high + perimeterPos;
                               high = aux;
                           } else {
                               float unusedB;
                               perimeterPos -= width;
                               if (perimeterPos <= low) {
                                   low = high;
                                   high = aux - perimeterPos;
                               } else {
                                   float unusedC;
                                   perimeterPos -= low;
                                   if (perimeterPos <= width) {
                                       low = high - perimeterPos;
                                       high = -aux;
                                   } else {
                                       low = -high;
                                       high = -aux + (perimeterPos -= width);
                                   }
                               }
                           }
                           particle.position.set(position).add(low, high);
                       }
                   }
                   break;
               }
               case SpawnShape.ELLIPSE: {
                   float edgeX;
                   float angle;
                   low = this.useLowEllipseWidth2 ? GameUtils.randomFloat(this.lowEllipseWidth1, this.lowEllipseWidth2) : this.lowEllipseWidth1;
                   high = this.useHighEllipseWidth2 ? GameUtils.randomFloat(this.highEllipseWidth1, this.highEllipseWidth2) : this.highEllipseWidth1;
                   width = BezierUtil.evalCurve(this.durationPercent, this.ellipseWidthCurve, low, high) * scale;
                   aux = this.useLowEllipseHeight2 ? GameUtils.randomFloat(this.lowEllipseHeight1, this.lowEllipseHeight2) : this.lowEllipseHeight1;
                   low = this.useHighEllipseHeight2 ? GameUtils.randomFloat(this.highEllipseHeight1, this.highEllipseHeight2) : this.highEllipseHeight1;
                   low = BezierUtil.evalCurve(this.durationPercent, this.ellipseHeightCurve, aux, low) * scale;
                   high = width * 0.5f;
                   if (high == 0.0f && (low *= 0.5f) == 0.0f) {
                       particle.position.set(position);
                       break;
                   }
                   switch (this.ellipseSpawnMode) {
                       case EllipseSpawnMode.FILL: {
                           double fillAngle = GameUtils.randomFloat(0.0f, (float)Math.PI * 2);
                           width = (float)Math.sqrt(GameUtils.randomFloat(0.0f, 1.0f));
                           high = (float)(Math.cos(fillAngle) * (double)high * (double)width);
                           low = (float)(Math.sin(fillAngle) * (double)low * (double)width);
                           particle.position.set(position).add(high, low);
                           break block5;
                       }
                       case EllipseSpawnMode.EDGE: {
                           if (high == 0.0f) {
                               angle = GameUtils.randomFloat(-low, low);
                               particle.position.set(position).add(0.0f, angle);
                               break block5;
                           }
                           if (low == 0.0f) {
                               angle = GameUtils.randomFloat(-high, high);
                               particle.position.set(position).add(angle, 0.0f);
                               break block5;
                           }
                           angle = GameUtils.randomFloat(0.0f, (float)Math.PI * 2);
                           edgeX = (float)(Math.cos(angle) * (double)high);
                           width = (float)(Math.sin(angle) * (double)low);
                           particle.position.set(position).add(edgeX, width);
                           break block5;
                       }
                       case EllipseSpawnMode.EDGE_TOP: {
                           if (high == 0.0f) {
                               particle.position.set(position).add(0.0f, low);
                               break block5;
                           }
                           if (low == 0.0f) {
                               angle = GameUtils.randomFloat(-high, high);
                               particle.position.set(position).add(angle, 0.0f);
                               break block5;
                           }
                           angle = GameUtils.randomFloat(0.0f, (float)Math.PI);
                           edgeX = (float)(Math.cos(angle) * (double)high);
                           width = (float)(Math.sin(angle) * (double)low);
                           particle.position.set(position).add(edgeX, width);
                           break block5;
                       }
                       case EllipseSpawnMode.EDGE_BOTTOM: {
                           if (high == 0.0f) {
                               particle.position.set(position).add(0.0f, -low);
                               break block5;
                           }
                           if (low == 0.0f) {
                               angle = GameUtils.randomFloat(-high, high);
                               particle.position.set(position).add(angle, 0.0f);
                               break block5;
                           }
                           angle = GameUtils.randomFloat((float)Math.PI, (float)Math.PI * 2);
                           edgeX = (float)(Math.cos(angle) * (double)high);
                           width = (float)(Math.sin(angle) * (double)low);
                           particle.position.set(position).add(edgeX, width);
                           break block5;
                       }
                       case EllipseSpawnMode.EDGE_LEFT: {
                           if (low == 0.0f) {
                               particle.position.set(position).add(-high, 0.0f);
                               break block5;
                           }
                           if (high == 0.0f) {
                               angle = GameUtils.randomFloat(-low, low);
                               particle.position.set(position).add(0.0f, angle);
                               break block5;
                           }
                           angle = GameUtils.randomFloat(1.5707964f, 4.712389f);
                           edgeX = (float)(Math.cos(angle) * (double)high);
                           width = (float)(Math.sin(angle) * (double)low);
                           particle.position.set(position).add(edgeX, width);
                           break block5;
                       }
                       case EllipseSpawnMode.EDGE_RIGHT: {
                           if (low == 0.0f) {
                               particle.position.set(position).add(high, 0.0f);
                               break block5;
                           }
                           if (high == 0.0f) {
                               angle = GameUtils.randomFloat(-low, low);
                               particle.position.set(position).add(0.0f, angle);
                               break block5;
                           }
                           angle = GameUtils.randomFloat(-1.5707964f, 1.5707964f);
                           edgeX = (float)(Math.cos(angle) * (double)high);
                           width = (float)(Math.sin(angle) * (double)low);
                           particle.position.set(position).add(edgeX, width);
                       }
                   }
               }
           }
           low = this.useLowLife2 ? GameUtils.randomFloat(this.lowLife1, this.lowLife2) : this.lowLife1;
           high = this.useHighLife2 ? GameUtils.randomFloat(this.highLife1, this.highLife2) : this.highLife1;
           particle.lifeDuration = BezierUtil.evalCurve(this.durationPercent, this.lifeCurve, low, high);
           particle.attached = this.attached;
           low = (this.useLowSizeX2 ? GameUtils.randomFloat(this.lowSizeX1, this.lowSizeX2) : this.lowSizeX1) * scale;
           high = (this.useHighSizeX2 ? GameUtils.randomFloat(this.highSizeX1, this.highSizeX2) : this.highSizeX1) * scale;
           width = (this.useLowSizeY2 ? GameUtils.randomFloat(this.lowSizeY1, this.lowSizeY2) : this.lowSizeY1) * scale;
           aux = (this.useHighSizeY2 ? GameUtils.randomFloat(this.highSizeY1, this.highSizeY2) : this.highSizeY1) * scale;
           particle.sizeXLow = low;
           particle.sizeXHigh = high;
           particle.sizeYLow = this.splitXAndY ? width : low;
           particle.sizeYHigh = this.splitXAndY ? aux : high;
           particle.rotationLow = this.useLowRotation2 ? GameUtils.randomFloat(this.lowRotation1, this.lowRotation2) : this.lowRotation1;
           particle.rotationHigh = this.useHighRotation2 ? GameUtils.randomFloat(this.highRotation1, this.highRotation2) : this.highRotation1;
           particle.velocityLow = (this.useLowVelocity2 ? GameUtils.randomFloat(this.lowVelocity1, this.lowVelocity2) : this.lowVelocity1) * scale;
           particle.velocityHigh = (this.useHighVelocity2 ? GameUtils.randomFloat(this.highVelocity1, this.highVelocity2) : this.highVelocity1) * scale;
           particle.angleLow = this.useLowAngle2 ? GameUtils.randomFloat(this.lowAngle1, this.lowAngle2) : this.lowAngle1;
           particle.angleHigh = this.useHighAngle2 ? GameUtils.randomFloat(this.highAngle1, this.highAngle2) : this.highAngle1;
           particle.gravityLow = (this.useLowGravity2 ? GameUtils.randomFloat(this.lowGravity1, this.lowGravity2) : this.lowGravity1) * scale;
           particle.gravityHigh = (this.useHighGravity2 ? GameUtils.randomFloat(this.highGravity1, this.highGravity2) : this.highGravity1) * scale;
           particle.windLow = (this.useLowWind2 ? GameUtils.randomFloat(this.lowWind1, this.lowWind2) : this.lowWind1) * scale;
           particle.windHigh = (this.useHighWind2 ? GameUtils.randomFloat(this.highWind1, this.highWind2) : this.highWind1) * scale;
           int lowTrailMaxPoints = this.useLowTrailMaxPoints2 ? GameUtils.randomInt(this.lowTrailMaxPoints1, this.lowTrailMaxPoints2) : this.lowTrailMaxPoints1;
           int highTrailMaxPoints = this.useHighTrailMaxPoints2 ? GameUtils.randomInt(this.highTrailMaxPoints1, this.highTrailMaxPoints2) : this.highTrailMaxPoints1;
           particle.trailMaxPointsLow = lowTrailMaxPoints;
           particle.trailMaxPointsHigh = highTrailMaxPoints;
           low = this.useLowTrailStepTime2 ? GameUtils.randomFloat(this.lowTrailStepTime1, this.lowTrailStepTime2) : this.lowTrailStepTime1;
           high = this.useHighTrailStepTime2 ? GameUtils.randomFloat(this.highTrailStepTime1, this.highTrailStepTime2) : this.highTrailStepTime1;
           particle.trailStepTimeLow = low;
           particle.trailStepTimeHigh = high;
           low = this.useLowTrailAlpha2 ? GameUtils.randomFloat(this.lowTrailAlpha1, this.lowTrailAlpha2) : this.lowTrailAlpha1;
           high = this.useHighTrailAlpha2 ? GameUtils.randomFloat(this.highTrailAlpha1, this.highTrailAlpha2) : this.highTrailAlpha1;
           particle.trailAlphaLow = low;
           particle.trailAlphaHigh = high;
           low = this.useLowBloomIntensity2 ? GameUtils.randomFloat(this.lowBloomIntensity1, this.lowBloomIntensity2) : this.lowBloomIntensity1;
           high = this.useHighBloomIntensity2 ? GameUtils.randomFloat(this.highBloomIntensity1, this.highBloomIntensity2) : this.highBloomIntensity1;
           particle.bloomIntensityLow = low;
           particle.bloomIntensityHigh = high;
           low = this.useLowBloomSizeMultiplier2 ? GameUtils.randomFloat(this.lowBloomSizeMultiplier1, this.lowBloomSizeMultiplier2) : this.lowBloomSizeMultiplier1;
           high = this.useHighBloomSizeMultiplier2 ? GameUtils.randomFloat(this.highBloomSizeMultiplier1, this.highBloomSizeMultiplier2) : this.highBloomSizeMultiplier1;
           particle.bloomSizeMultLow = low;
           particle.bloomSizeMultHigh = high;
           this.particles.add(particle);
       }
   }

   public static final class EllipseSpawnMode {
      public static final int EDGE = 1;
      public static final int FILL = 2;
      public static final int EDGE_TOP = 3;
      public static final int EDGE_BOTTOM = 4;
      public static final int EDGE_LEFT = 5;
      public static final int EDGE_RIGHT = 6;
      public static final int[] VALUES;

      public static int[] getValues() {
         return (int[])VALUES.clone();
      }

      static {
         int[] values = new int[6];
         values[0] = EDGE;
         values[1] = FILL;
         values[2] = EDGE_TOP;
         values[3] = EDGE_BOTTOM;
         values[4] = EDGE_LEFT;
         values[5] = EDGE_RIGHT;
         VALUES = values;
      }
   }

   public static final class SpawnShape {
      public static final int POINT = 1;
      public static final int LINE = 2;
      public static final int SQUARE = 3;
      public static final int ELLIPSE = 4;
      public static final int[] VALUES;

      public static int[] getValues() {
         return (int[])VALUES.clone();
      }

      static {
         int[] values = new int[4];
         values[0] = POINT;
         values[1] = LINE;
         values[2] = SQUARE;
         values[3] = ELLIPSE;
         VALUES = values;
      }
   }

   public static final class SpriteMode {
      public static final int SINGLE = 1;
      public static final int RANDOM = 2;
      public static final int ANIMATED = 3;
      public static final int[] VALUES;

      public static int[] getValues() {
         return (int[])VALUES.clone();
      }

      static {
         int[] values = new int[3];
         values[0] = SINGLE;
         values[1] = RANDOM;
         values[2] = ANIMATED;
         VALUES = values;
      }
   }

   public static final class SquareSpawnMode {
      public static final int EDGE = 1;
      public static final int FILL = 2;
      public static final int[] VALUES;

      public static int[] getValues() {
         return (int[])VALUES.clone();
      }

      static {
         int[] values = new int[2];
         values[0] = EDGE;
         values[1] = FILL;
         VALUES = values;
      }
   }
}
