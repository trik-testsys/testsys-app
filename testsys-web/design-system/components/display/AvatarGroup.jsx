import React from 'react';
import { Avatar } from './Avatar.jsx';
export function AvatarGroup({ names = [], max = 3, size = 32 }) {
  const rest = names.length - max;
  return (
    <div className="ts-avatars">
      {names.slice(0, max).map(n => <Avatar key={n} name={n} size={size} />)}
      {rest > 0 ? <Avatar initials={'+' + rest} tone={3} size={size} /> : null}
    </div>
  );
}
