Countdown to start/end of a contest or quiz question; always JetBrains Mono.
```jsx
<Timer to={endsAt} variant="chip" />
<Block dark><Timer to={endsAt} variant="hero" /></Block>
<Timer to={startsAt} variant="tiles" />
```
Also exports `useCountdown(to)` returning remaining seconds.
