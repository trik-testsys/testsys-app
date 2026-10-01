// Bounds are relative to the containing header, with a visible inset on both edges.
export function getPopoverBounds(header, trigger, requestedWidth = 320, inset = 12) {
  const edge = Math.min(inset, Math.max(0, header.width / 2));
  const width = Math.max(0, Math.min(requestedWidth, header.width - edge * 2));
  const right = Math.min(Math.max(edge, header.right - trigger.right), Math.max(edge, header.width - width - edge));
  return { right, width };
}
