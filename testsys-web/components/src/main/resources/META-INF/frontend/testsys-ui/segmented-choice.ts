interface ChoiceRadio extends HTMLElement {
  disabled: boolean;
}

interface ChoiceGroup extends HTMLElement {
  disabled: boolean;
  readonly: boolean;
}

const active = new WeakMap<ChoiceGroup, (event: KeyboardEvent) => void>();

export const segmentedChoice = {
  attach(group: ChoiceGroup) {
    this.detach(group);
    const onKeyDown = (event: KeyboardEvent) => {
      if (!['Home', 'End'].includes(event.key) || group.disabled || group.readonly) return;
      const radios = [...group.querySelectorAll<ChoiceRadio>('vaadin-radio-button')].filter(radio => !radio.disabled);
      if (!radios.length || !event.composedPath().some(target => radios.some(radio => radio === target))) return;
      event.preventDefault();
      const radio = event.key === 'Home' ? radios[0] : radios[radios.length - 1];
      radio.click();
      radio.focus();
    };
    group.addEventListener('keydown', onKeyDown);
    active.set(group, onKeyDown);
  },
  detach(group: ChoiceGroup) {
    const onKeyDown = active.get(group);
    if (onKeyDown) group.removeEventListener('keydown', onKeyDown);
    active.delete(group);
  }
};

declare global {
  interface Window {
    testsysSegmentedChoice: typeof segmentedChoice;
  }
}

if (typeof window !== 'undefined') window.testsysSegmentedChoice = segmentedChoice;
