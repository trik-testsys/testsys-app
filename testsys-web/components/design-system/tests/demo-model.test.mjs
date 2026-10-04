import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createDemoState, reduceDemoState, getAllowedSolutionKinds, getBestScore, buildResultsCsv, getCabinetScreen } from '../ui_kits/platform/DemoModel.mjs';

test('should access accepts a valid code and rejects unknown codes', () => {
  const initial = createDemoState();
  assert.equal(reduceDemoState(initial, { type: 'login', code: 'wrong' }).sessionUserId, null);
  assert.equal(reduceDemoState(initial, { type: 'login', code: 'STUDENT-2026' }).sessionUserId, 'student');
});
test('should registration detects occupied email and validates confirmation', () => {
  const initial = createDemoState();
  assert.equal(reduceDemoState(initial, { type: 'register', alias: 'Тест', email: 'ANNA@example.com', role: 'Ученик' }).pendingRegistration, null);
  const pending = reduceDemoState(initial, { type: 'register', alias: 'Тест', email: 'new@example.com', role: 'Ученик' });
  assert.equal(reduceDemoState(pending, { type: 'confirmRegistration', code: 'wrong' }).users.length, 9);
  const registered = reduceDemoState(pending, { type: 'confirmRegistration', code: '246810' });
  assert.equal(registered.users.at(-1).accessCode, 'ACCESS-103');
  assert.equal(registered.users.at(-1).role, 'Ученик');
  assert.equal(registered.sessionUserId, 'user103');
});
test('should registration rejects incomplete fields', () => {
  assert.equal(reduceDemoState(createDemoState(), { type: 'register', alias: '', email: 'bad', role: 'Ученик' }).message.tone, 'danger');
});
test('should recovery replaces access and its link cannot be reused', () => {
  const requested = reduceDemoState(createDemoState(), { type: 'requestRecovery', email: 'anna@example.com' });
  const recovered = reduceDemoState(requested, { type: 'restoreAccess', token: requested.recovery.token });
  assert.equal(reduceDemoState(recovered, { type: 'login', code: 'STUDENT-2026' }).sessionUserId, null);
  assert.equal(reduceDemoState(recovered, { type: 'login', code: 'RESTORED-103' }).sessionUserId, 'student');
  assert.equal(reduceDemoState(recovered, { type: 'restoreAccess', token: requested.recovery.token }).message.tone, 'danger');
});
test('should allowed kinds come only from author solutions', () => {
  assert.deepEqual(getAllowedSolutionKinds(createDemoState().tasks[2]), ['VisualLanguage']);
});
test('should fixture links select only the class tours and their tasks', () => {
  const state = createDemoState();
  assert.deepEqual(state.classes[1].tourIds, ['t2']);
  assert.deepEqual(state.tours.find(t => t.id === 't2').taskIds, ['task3']);
  assert.deepEqual(state.classes[2].tourIds, []);
});
test('should best score separates zero from absence and ignores pending statuses', () => {
  const rows = [{ userId: 'u', taskId: 't', score: 0, status: 'Checked' }, { userId: 'u', taskId: 't', score: 90, status: 'Checking' }];
  assert.equal(getBestScore(rows, 'u', 't'), 0);
  assert.equal(getBestScore(rows, 'other', 't'), null);
});
test('should creating participants updates competition membership and unique codes', () => {
  const created = reduceDemoState(createDemoState(), { type: 'createParticipants', competitionId: 'competition1' });
  assert.deepEqual(created.competitions[0].participantIds, ['p1', 'p2', 'p103', 'p104', 'p105']);
  assert.deepEqual(created.users.slice(-3).map(u => u.accessCode), ['PART-103', 'PART-104', 'PART-105']);
});
test('should new competition has an owner and no initial participants or tours', () => {
  const created = reduceDemoState(createDemoState(), { type: 'createCompetition', name: 'Новый турнир' });
  assert.deepEqual(created.competitions.at(-1), { id: 'competition103', name: 'Новый турнир', organizerId: 'organizer', communityId: 'community1', participantIds: [], tourIds: [] });
});
test('should adding a tour never duplicates its membership', () => {
  const once = reduceDemoState(createDemoState(), { type: 'addTour', competitionId: 'competition1', tourId: 't2' });
  const twice = reduceDemoState(once, { type: 'addTour', competitionId: 'competition1', tourId: 't2' });
  assert.deepEqual(twice.competitions[0].tourIds, ['t1', 't2']);
  assert.equal(twice.message.tone, 'danger');
});
test('should submission validates access and kind before preserving task identity across checking', () => {
  const initial = createDemoState();
  const denied = reduceDemoState(initial, { type: 'submitSolution', userId: 'p1', taskId: 'task3', kind: 'VisualLanguage', fileName: 'demo.qrs' });
  assert.equal(denied.solutions.length, 4);
  const wrongKind = reduceDemoState(initial, { type: 'submitSolution', userId: 'student', taskId: 'task1', kind: 'JavaScript', fileName: 'demo.js' });
  assert.equal(wrongKind.solutions.length, 4);
  const queued = reduceDemoState(initial, { type: 'submitSolution', userId: 'student', taskId: 'task2', kind: 'JavaScript', fileName: 'sensor.js' });
  const checking = reduceDemoState(queued, { type: 'checkSolution', solutionId: 's103' });
  const completed = reduceDemoState(checking, { type: 'finishSolution', solutionId: 's103', score: 0 });
  assert.equal(completed.solutions.at(-1).taskId, 'task2');
  assert.equal(completed.solutions.at(-1).status, 'Checked');
  assert.equal(getBestScore(completed.solutions, 'student', 'task2'), 0);
});
test('should CSV includes full matrix, absent scores, counts and escapes delimiters quotes and newlines', () => {
  const state = createDemoState();
  state.users.find(u => u.id === 'p1').alias = 'Имя; "кавычки"\nперенос';
  const csv = buildResultsCsv(state, 'competition1', 't1');
  assert.equal(csv, '\uFEFF"ID";"Псевдоним";"task1 Движение по линии: лучший балл";"task1 Движение по линии: решений";"task2 Датчик расстояния: лучший балл";"task2 Датчик расстояния: решений"\r\n"p1";"Имя; ""кавычки""\nперенос";"85";"1";"";"0"\r\n"p2";"Участник 2";"";"0";"";"1"');
});

test('should keep a registered organizer isolated from the fixture organizer', () => {
  const pending = reduceDemoState(createDemoState(), { type: 'register', alias: 'Новый организатор', email: 'owner@example.com', role: 'Организатор' });
  const registered = reduceDemoState(pending, { type: 'confirmRegistration', code: '246810' });
  assert.equal(registered.competitions.filter(c => c.organizerId === registered.sessionUserId).length, 0);
  assert.equal(reduceDemoState(registered, { type: 'createParticipants', competitionId: 'competition1' }).message.tone, 'danger');
  const created = reduceDemoState(registered, { type: 'createCompetition', name: 'Своё соревнование' });
  assert.equal(created.competitions.at(-1).organizerId, 'user103');
  const members = reduceDemoState(created, { type: 'createParticipants', competitionId: 'competition104' });
  assert.deepEqual(members.competitions.at(-1).participantIds, ['p105', 'p106', 'p107']);
  const tour = reduceDemoState(members, { type: 'addTour', competitionId: 'competition104', tourId: 't1' });
  assert.deepEqual(tour.competitions.at(-1).tourIds, ['t1']);
  assert.deepEqual(tour.competitions[0].participantIds, ['p1', 'p2']);
});

test('should preserve participant identity without substituting a student cabinet', () => {
  const signedIn = reduceDemoState(createDemoState(), { type: 'login', code: 'PART-101' });
  assert.equal(signedIn.sessionUserId, 'p1');
  assert.equal(getCabinetScreen(signedIn.users.find(u => u.id === signedIn.sessionUserId).role), 'participant.overview');
  assert.equal(getCabinetScreen('Ученик'), 'student.overview');
  assert.equal(getCabinetScreen('Организатор'), 'organizer.overview');
});

test('should create the requested number of participants and advance the ID sequence', () => {
  const initial = createDemoState();
  const created = reduceDemoState(initial, { type: 'createParticipants', competitionId: 'competition1', count: 7 });
  assert.equal(created.users.length - initial.users.length, 7);
  assert.equal(created.nextId, initial.nextId + 7);
  assert.equal(new Set(created.users.map(user => user.id)).size, created.users.length);
  assert.equal(new Set(created.users.map(user => user.accessCode)).size, created.users.length);
  assert.equal(created.competitions[0].participantIds.length - initial.competitions[0].participantIds.length, 7);
});
for (const count of [0, -1, 1.5, 101, NaN, '4']) {
  test(`should reject invalid demo participant count ${String(count)} without creating records`, () => {
    const initial = createDemoState();
    const rejected = reduceDemoState(initial, { type: 'createParticipants', competitionId: 'competition1', count });
    assert.deepEqual(rejected.users, initial.users);
    assert.deepEqual(rejected.competitions, initial.competitions);
    assert.equal(rejected.nextId, initial.nextId);
    assert.equal(rejected.message.tone, 'danger');
  });
}
