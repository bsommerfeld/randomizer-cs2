package de.bsommerfeld.randomizer.ui.view.component;

import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Setter;
import org.controlsfx.control.RangeSlider;

public class MinMaxSlider extends HBox {

  private final RangeSlider rangeSlider = new RangeSlider(0, 300, 30, 100); // max 5 minutes
  private final Label minLabel = new Label();
  private final Label maxLabel = new Label();

  @Setter private TimeUnit timeUnit = TimeUnit.SECONDS;

  private boolean showLabels = true;
  private boolean initialized = false;

  private Consumer<Double> minValueChangeConsumer;
  private Consumer<Double> maxValueChangeConsumer;

  public MinMaxSlider() {
    Platform.runLater(this::initialize);
  }

  public boolean isEven() {
    return rangeSlider.getLowValue() == rangeSlider.getHighValue();
  }

  public DoubleProperty getMinProperty() {
    return rangeSlider.lowValueProperty();
  }

  public DoubleProperty getMaxProperty() {
    return rangeSlider.highValueProperty();
  }

  public void setOnMinValueChange(Consumer<Double> consumer) {
    minValueChangeConsumer = consumer;
  }

  public void setOnMaxValueChange(Consumer<Double> consumer) {
    maxValueChangeConsumer = consumer;
  }

  public void setMaxHigherValue(int value) {
    rangeSlider.setMax(value);
  }

  public void setMinLowerValue(int value) {
    rangeSlider.setMin(value);
  }

  public void showLabels(boolean show) {
    this.showLabels = show;

    if (initialized) {
      updateLabelVisibility();
    }
  }

  private void updateLabelVisibility() {
    if (showLabels) {
      if (!getChildren().contains(minLabel) || !getChildren().contains(maxLabel)) {
        getChildren()
            .setAll(
                getButtons(ButtonType.MIN),
                minLabel,
                rangeSlider,
                maxLabel,
                getButtons(ButtonType.MAX));
      }
    } else {
      getChildren().setAll(getButtons(ButtonType.MIN), rangeSlider, getButtons(ButtonType.MAX));
    }
  }

  public void setMinMaxValue(int min, int max) {
    minLabel.setText(min + timeUnit.getLabel());
    maxLabel.setText(max + timeUnit.getLabel());
    rangeSlider.setLowValue(min);
    rangeSlider.setHighValue(max);
  }

  private void initialize() {
    initializeLabels();
    setAlignment(Pos.CENTER);

    updateLabelVisibility();

    rangeSlider.setBlockIncrement(1);
    setListener();
    initialized = true;
  }

  private VBox getButtons(ButtonType type) {
    Button upButton = createButton("rangeslider-up-button");
    Button downButton = createButton("rangeslider-down-button");

    switch (type) {
      case MIN:
        upButton.setOnAction(_ -> rangeSlider.setLowValue(rangeSlider.getLowValue() + 1));
        downButton.setOnAction(_ -> rangeSlider.setLowValue(rangeSlider.getLowValue() - 1));
        break;
      case MAX:
        upButton.setOnAction(_ -> rangeSlider.setHighValue(rangeSlider.getHighValue() + 1));
        downButton.setOnAction(_ -> rangeSlider.setHighValue(rangeSlider.getHighValue() - 1));
        break;
    }

    VBox vBox = new VBox(upButton, downButton);
    vBox.setAlignment(Pos.CENTER);
    return vBox;
  }

  private Button createButton(String styleClass) {
    Button button = new Button();
    button.getStyleClass().add(styleClass);
    return button;
  }

  private void initializeLabels() {
    minLabel.getStyleClass().add("rangeslider-min-label");
    maxLabel.getStyleClass().add("rangeslider-max-label");
  }

  private void setListener() {
    rangeSlider
        .lowValueProperty()
        .addListener(
            (_, _, newValue) -> {
              minLabel.setText(newValue.intValue() + timeUnit.getLabel());
              if (minValueChangeConsumer != null) {
                minValueChangeConsumer.accept(newValue.doubleValue());
              }
            });

    rangeSlider
        .highValueProperty()
        .addListener(
            (_, _, newValue) -> {
              maxLabel.setText(newValue.intValue() + timeUnit.getLabel());
              if (maxValueChangeConsumer != null) {
                maxValueChangeConsumer.accept(newValue.doubleValue());
              }
            });
  }

  private enum ButtonType {
    MIN,
    MAX
  }

  public enum TimeUnit {
    MILLISECONDS("ms"),
    SECONDS("s");

    private final String label;

    TimeUnit(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }
  }
}
