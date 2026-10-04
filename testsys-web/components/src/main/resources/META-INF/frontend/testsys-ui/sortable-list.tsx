import React from 'react';
import {ConnectedReactAdapterElement} from './connected-react-adapter';
import {type RenderHooks} from 'Frontend/generated/flow/ReactAdapter';
import {ServerSnapshot} from './server-snapshot';
import {SortableList} from './SortableList.jsx';
function Slot({name, hooks}: {name: string, hooks: RenderHooks}) { return hooks.useContent(name); }
class SortableAdapter extends ConnectedReactAdapterElement {
  private readonly snapshot = new ServerSnapshot<any>(this, 'list');
  protected render(hooks: RenderHooks) {
    const data = this.snapshot.useSnapshot();
    const changed = hooks.useCustomEvent<any>('list-reorder');
    if (!data) return null;
    return <SortableList items={data.items} getKey={(item: any) => item.key} disabled={data.disabled}
      handleLabel={data.handleLabel} announcement={(item: any, position: number) => data.positions[item.key]?.[position-1] ?? item.announcement} renderItem={(item: any) => <Slot key={item.slot} name={item.slot} hooks={hooks} />}
      renderDragItem={(item: any) => item.label}
      onChange={(items: any[]) => changed({version: data.version, keys: items.map(item => item.key)})} />;
  }
}
customElements.define('testsys-sortable-list', SortableAdapter);
