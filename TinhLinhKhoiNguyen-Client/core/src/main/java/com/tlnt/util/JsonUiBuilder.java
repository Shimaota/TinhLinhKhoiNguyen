package com.tlnt.util;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public final class JsonUiBuilder {
   public Item[] items;

   public final void setItems(Item[] itemArray) {
      this.items = itemArray;
   }

   public final Table buildLayout(RenderContext ctx, String json) {
      JSONObject root = (JSONObject)JSONValue.parse(json);
      Table table = new Table().top();
      if (root == null) {
         return table;
      } else {
         this.applyActorProperties(table, (JSONObject)root.getOrDefault("properties", new JSONObject()));
         this.buildChildren(ctx, table, (JSONArray)root.getOrDefault("childrens", new JSONArray()));
         return table;
      }
   }

   public void buildChildren(RenderContext ctx, Table parent, JSONArray children) {
      if (parent != null && children != null) {
         if (parent instanceof Table) {
            Table parentTable = (Table)parent;
            Iterator it = children.iterator();

            while (it.hasNext()) {
               JSONObject child;
               String type = (child = (JSONObject)it.next()).getOrDefault("type", "").toString();
               String typeChildKey = "typeChild";
               int addMode = Integer.parseInt(child.getOrDefault(typeChildKey, Integer.valueOf(0)).toString());
               if (!type.isEmpty()) {
                  Object actor = null;
                  int typeIndex = -1;
                  switch (type.hashCode()) {
                     case -1085917679:
                        if (type.equals("slotitem")) {
                           typeIndex = 3;
                        }
                        break;
                     case 67115957:
                        if (type.equals("scrollpane")) {
                           typeIndex = 2;
                        }
                        break;
                     case 102727412:
                        if (type.equals("label")) {
                           typeIndex = 1;
                        }
                        break;
                     case 110115790:
                        if (type.equals("table")) {
                           typeIndex = 0;
                        }
                  }

                  switch (typeIndex) {
                     case 0:
                        actor = new JsonUiBuilder.TexturedTable(ctx);
                        this.buildChildren(ctx, (Table)actor, (JSONArray)child.getOrDefault("childrens", new JSONArray()));
                        break;
                     case 1:
                        actor = new TextraLabel();
                        break;
                     case 2:
                        Table scrollContent;
                        actor = ScrollPaneFactory.createScrollPane(scrollContent = new Table().top().left());
                        this.buildChildren(ctx, scrollContent, (JSONArray)child.getOrDefault("childrens", new JSONArray()));
                        break;
                     case 3:
                        ItemSlotButton slotButton;
                        (slotButton = (ItemSlotButton)(actor = new ItemSlotButton(ctx))).addListener(new ItemTooltipClickListener(this, slotButton));
                  }

                  if (actor != null) {
                     this.applyActorProperties((Actor)actor, (JSONObject)child.getOrDefault("properties", new JSONObject()));
                     switch (addMode) {
                        case 0:
                           parentTable.addActor((Actor)actor);
                           break;
                        case 1:
                           applyCellProperties(
                              parentTable.add((Actor)actor), (JSONObject)child.getOrDefault("cell", new JSONObject())
                           );
                     }
                  }
               }
            }
         }
      }
   }

   public void applyActorProperties(Actor actor, JSONObject props) {
      if (actor != null && props != null) {
         if (props.containsKey("size")) {
            JSONArray sizeArr;
            float width = Float.parseFloat(
               (sizeArr = (JSONArray)props.get("size")).get(0).toString()
            );
            float height = Float.parseFloat(sizeArr.get(1).toString());
            actor.setSize(width, height);
         } else {
            if (props.containsKey("width")) {
               actor.setWidth(Float.parseFloat(props.get("width").toString()));
            }

            if (props.containsKey("height")) {
               actor.setHeight(Float.parseFloat(props.get("height").toString()));
            }
         }

         if (props.containsKey("position")) {
            JSONArray posArr;
            float x = Float.parseFloat((posArr = (JSONArray)props.get("position")).get(0).toString());
            float y = Float.parseFloat(posArr.get(1).toString());
            actor.setPosition(x, y);
         } else {
            if (props.containsKey("x")) {
               actor.setX(Float.parseFloat(props.get("x").toString()));
            }

            if (props.containsKey("y")) {
               actor.setY(Float.parseFloat(props.get("y").toString()));
            }
         }

         if (actor instanceof JsonUiBuilder.TexturedTable) {
            JsonUiBuilder.TexturedTable texturedTable = (JsonUiBuilder.TexturedTable)actor;
            if (props.containsKey("fillParent")) {
               texturedTable.setFillParent(Boolean.parseBoolean(props.get("fillParent").toString()));
            }

            if (props.containsKey("top") && Boolean.parseBoolean(props.get("top").toString())) {
               texturedTable.top();
            }

            if (props.containsKey("bottom") && Boolean.parseBoolean(props.get("bottom").toString())) {
               texturedTable.bottom();
            }

            if (props.containsKey("center")) {
               if (Boolean.parseBoolean(props.get("center").toString())) {
                  texturedTable.center();
               }
            }

            if (props.containsKey("left") && Boolean.parseBoolean(props.get("left").toString())) {
               texturedTable.left();
            }

            if (props.containsKey("right")
               && Boolean.parseBoolean(props.get("right").toString())) {
               texturedTable.right();
            }

            if (props.containsKey("pad")) {
               JSONArray padArr;
               JSONArray padValues = padArr = (JSONArray)props.get("pad");
               float padTop = Float.parseFloat(padValues.get(0).toString());
               float padLeft = Float.parseFloat(padArr.get(1).toString());
               float padBottom = Float.parseFloat(padArr.get(2).toString());
               float padRight = Float.parseFloat(padArr.get(3).toString());
               texturedTable.pad(padTop, padLeft, padBottom, padRight);
            }

            if (props.containsKey("image")) {
               texturedTable.setBackgroundTexture(props.get("image").toString());
            }
         }

         if (actor instanceof TextraLabel) {
            TextraLabel label;
            TextraLabel textLabel = label = (TextraLabel)actor;
            textLabel.setText(props.getOrDefault("text", "").toString());
            label.setAlignment(
               Integer.parseInt(props.getOrDefault("align", Integer.valueOf(1)).toString())
            );
            label.setWrap(Boolean.parseBoolean(props.getOrDefault("wrap", Boolean.FALSE).toString()));
            String key = "font";
            int fontIndex = Integer.parseInt(props.getOrDefault(key, Integer.valueOf(1)).toString());
            key = "fontSize";
            int fontSize = Integer.parseInt(props.getOrDefault(key, Integer.valueOf(20)).toString());
            key = "adjustLineHeight";
            float lineHeightAdjust = Float.parseFloat(props.getOrDefault(key, Integer.valueOf(2)).toString());
            BitmapFont font = null;
            switch (fontIndex) {
               case 1:
                  font = FontManager.PLAIN_FONT;
                  break;
               case 2:
                  font = FontManager.DEFAULT_FONT;
            }

            if (font != null) {
               label.setFont(FontFactory.createTextraFontWithLineHeight(font, fontSize, lineHeightAdjust));
            }
         }

         if (actor instanceof ItemSlotButton) {
            ItemSlotButton slotButton = (ItemSlotButton)actor;
            String itemIdKey = "itemId";
            int itemIndex = Integer.parseInt(props.getOrDefault(itemIdKey, Integer.valueOf(-1)).toString());
            Item item = this.items != null && itemIndex >= 0 && itemIndex <= this.items.length - 1 ? this.items[itemIndex] : null;
            slotButton.setItem(item);
         }
      }
   }

   public static void applyCellProperties(Cell cell, JSONObject props) {
      if (cell != null && props != null) {
         if (props.containsKey("size")) {
            JSONArray sizeArr;
            float width = Float.parseFloat(
               (sizeArr = (JSONArray)props.get("size")).get(0).toString()
            );
            float height = Float.parseFloat(sizeArr.get(1).toString());
            cell.size(width, height);
         } else {
            if (props.containsKey("width")) {
               cell.width(Float.parseFloat(props.get("width").toString()));
            }

            if (props.containsKey("height")) {
               cell.height(Float.parseFloat(props.get("height").toString()));
            }
         }

         if (props.containsKey("minSize")) {
            JSONArray minSizeArr;
            JSONArray minSizeValues = minSizeArr = (JSONArray)props.get("minSize");
            float minWidth = Float.parseFloat(minSizeValues.get(0).toString());
            float minHeight = Float.parseFloat(minSizeArr.get(1).toString());
            cell.minSize(minWidth, minHeight);
         } else {
            if (props.containsKey("minWidth")) {
               cell.minWidth(Float.parseFloat(props.get("minWidth").toString()));
            }

            if (props.containsKey("minHeight")) {
               cell.minHeight(Float.parseFloat(props.get("minHeight").toString()));
            }
         }

         if (props.containsKey("maxSize")) {
            JSONArray maxSizeArr;
            JSONArray maxSizeValues = maxSizeArr = (JSONArray)props.get("maxSize");
            float maxWidth = Float.parseFloat(maxSizeValues.get(0).toString());
            float maxHeight = Float.parseFloat(maxSizeArr.get(1).toString());
            cell.maxSize(maxWidth, maxHeight);
         } else {
            if (props.containsKey("maxWidth")) {
               cell.maxWidth(Float.parseFloat(props.get("maxWidth").toString()));
            }

            if (props.containsKey("maxHeight")) {
               cell.maxHeight(Float.parseFloat(props.get("maxHeight").toString()));
            }
         }

         if (props.containsKey("pad")) {
            JSONArray padArr;
            JSONArray padValues = padArr = (JSONArray)props.get("pad");
            float padTop = Float.parseFloat(padValues.get(0).toString());
            float padLeft = Float.parseFloat(padArr.get(1).toString());
            float padBottom = Float.parseFloat(padArr.get(2).toString());
            float padRight = Float.parseFloat(padArr.get(3).toString());
            cell.pad(padTop, padLeft, padBottom, padRight);
         } else {
            float padValue;
            if (props.containsKey("padTop")) {
               padValue = Float.parseFloat(props.get("padTop").toString());
            } else {
               padValue = 0.0F;
            }

            float padTopValue = padValue;
            float padLeftValue = props.containsKey("padLeft") ? Float.parseFloat(props.get("padLeft").toString()) : 0.0F;
            float padBottomValue = props.containsKey("padBottom")
               ? Float.parseFloat(props.get("padBottom").toString())
               : 0.0F;
            if (props.containsKey("padRight")) {
               padValue = Float.parseFloat(props.get("padRight").toString());
            } else {
               padValue = 0.0F;
            }

            float padRightValue = padValue;
            if (padTopValue != 0.0F || padLeftValue != 0.0F || padBottomValue != 0.0F || padRightValue != 0.0F) {
               cell.pad(padTopValue, padLeftValue, padBottomValue, padRightValue);
            }
         }

         if (props.containsKey("space")) {
            JSONArray spaceArr;
            JSONArray spaceValues = spaceArr = (JSONArray)props.get("space");
            float spaceTop = Float.parseFloat(spaceValues.get(0).toString());
            float spaceLeft = Float.parseFloat(spaceArr.get(1).toString());
            float spaceBottom = Float.parseFloat(spaceArr.get(2).toString());
            float spaceRight = Float.parseFloat(spaceArr.get(3).toString());
            cell.space(spaceTop, spaceLeft, spaceBottom, spaceRight);
         } else {
            float spaceValue;
            if (props.containsKey("spaceTop")) {
               spaceValue = Float.parseFloat(props.get("spaceTop").toString());
            } else {
               spaceValue = 0.0F;
            }

            float spaceTopValue = spaceValue;
            if (props.containsKey("spaceLeft")) {
               spaceValue = Float.parseFloat(props.get("spaceLeft").toString());
            } else {
               spaceValue = 0.0F;
            }

            float spaceLeftValue = spaceValue;
            float spaceBottomValue = props.containsKey("spaceBottom") ? Float.parseFloat(props.get("spaceBottom").toString()) : 0.0F;
            float spaceRightValue = props.containsKey("spaceRight")
               ? Float.parseFloat(props.get("spaceRight").toString())
               : 0.0F;
            if (spaceTopValue != 0.0F || spaceLeftValue != 0.0F || spaceBottomValue != 0.0F || spaceRightValue != 0.0F) {
               cell.space(spaceTopValue, spaceLeftValue, spaceBottomValue, spaceRightValue);
            }
         }

         if (props.containsKey("grow")) {
            if (Boolean.parseBoolean(props.get("grow").toString())) {
               cell.grow();
            }
         } else {
            if (Boolean.parseBoolean(String.valueOf(props.getOrDefault("growX", Boolean.FALSE)))) {
               cell.growX();
            }

            if (Boolean.parseBoolean(String.valueOf(props.getOrDefault("growY", Boolean.FALSE)))) {
               cell.growY();
            }
         }

         if (props.containsKey("expand")) {
            if (Boolean.parseBoolean(props.get("expand").toString())) {
               cell.expand();
            }
         } else {
            if (Boolean.parseBoolean(String.valueOf(props.getOrDefault("expandX", Boolean.FALSE)))) {
               cell.expandX();
            }

            if (Boolean.parseBoolean(String.valueOf(props.getOrDefault("expandY", Boolean.FALSE)))) {
               cell.expandY();
            }
         }

         if (props.containsKey("fill")) {
            if (Boolean.parseBoolean(props.get("fill").toString())) {
               cell.fill();
            }
         } else {
            if (Boolean.parseBoolean(String.valueOf(props.getOrDefault("fillX", Boolean.FALSE)))) {
               cell.fillX();
            }

            if (Boolean.parseBoolean(String.valueOf(props.getOrDefault("fillY", Boolean.FALSE)))) {
               cell.fillY();
            }
         }

         if (props.containsKey("colspan")) {
            cell.colspan(Integer.parseInt(props.get("colspan").toString()));
         }

         if (props.containsKey("row") && Boolean.parseBoolean(props.get("row").toString())) {
            cell.row();
         }
      }
   }

   public class TexturedTable extends BaseTable {
      public TextureAsset backgroundTexture;

      public TexturedTable(RenderContext ctx) {
         super(ctx);
      }

      public final void setBackgroundTexture(String textureName) {
         this.backgroundTexture = TextureAsset.obtain(textureName);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
