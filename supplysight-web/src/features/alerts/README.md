# Alerts Feature - Testing Guide

## Overview

The Alerts feature provides real-time monitoring and management of shipment alerts and exceptions. It includes:

- Real-time SSE stream with polling fallback
- Notification bell with dropdown
- Toast notifications for high-severity alerts
- Full alerts list with filtering, sorting, and pagination
- Alert detail modal with actions
- Bulk operations (acknowledge/resolve)

---

## Prerequisites

1. **Backend services running:**
   ```bash
   # From project root
   cd tools
   ./start-all-services.ps1
   ```

2. **Frontend dev server:**
   ```bash
   cd supplysight-web
   npm run dev
   ```

3. **User credentials:**
   - Login as `ADMIN` or `OPS_USER` to access alerts features
   - `VIEWER` role has read-only access

---

## Feature Testing Checklist

### 1. Real-Time Stream & Notification Bell

**Test Steps:**
1. Navigate to any page (Dashboard, Shipments, etc.)
2. Look for the bell icon in the top-right header
3. Verify:
   - [ ] Bell icon is visible
   - [ ] Connection status dot is green (connected)
   - [ ] Unread count badge shows correct number
   - [ ] Badge shows "99+" for counts over 99

**Bell Dropdown:**
1. Click the bell icon
2. Verify:
   - [ ] Dropdown opens with recent alerts
   - [ ] Up to 10 most recent alerts shown
   - [ ] Each alert shows severity, type, and time
   - [ ] "View All" link navigates to `/alerts`
   - [ ] Empty state shown if no alerts

**Connection Status:**
- Green dot = Connected via SSE
- Yellow dot = Reconnecting
- Red dot = Disconnected (using polling fallback)

---

### 2. Toast Notifications

**Test Steps:**
1. Wait for a high-severity alert (CRITICAL) to appear
2. Or trigger one via backend/Kafka
3. Verify:
   - [ ] Toast appears in bottom-right corner
   - [ ] Shows severity badge, alert type, and message
   - [ ] Auto-dismisses after 5 seconds
   - [ ] Progress bar shows time remaining
   - [ ] Can manually dismiss with X button
   - [ ] Multiple toasts stack (max 3 visible)

**High-Severity Alerts:**
- `CRITICAL` severity
- `DELAY_RISK_HIGH` type
- `ANOMALY_DETECTED` type

---

### 3. Alerts List Page

**Navigation:**
```
/alerts
```

**Summary Cards:**
Verify all cards display correct counts:
- [ ] Total Alerts
- [ ] Open (unacknowledged)
- [ ] Acknowledged
- [ ] Avg Response Time (placeholder)

**Filters:**
- [ ] Search input filters by message, ID, or shipment ID
- [ ] Severity multi-select (CRITICAL, WARNING, INFO)
- [ ] Status multi-select (OPEN, ACKNOWLEDGED, RESOLVED)
- [ ] Advanced filters toggle reveals:
  - Alert type multi-select
  - Date range picker (from/to)
- [ ] Active filters show as chips below
- [ ] Can remove individual filters by clicking X on chip
- [ ] "Reset" button clears all filters

**Data Table:**
- [ ] Columns: Checkbox, Severity, Alert, Shipment, Created, Status
- [ ] Sortable columns (Severity, Created, Status) - click to toggle asc/desc
- [ ] Row selection checkboxes work
- [ ] "Select all" checkbox toggles all rows
- [ ] Severity badges color-coded (red/yellow/blue)
- [ ] Shipment links open in shipment detail page
- [ ] Status indicators (Open = yellow pulse, Acknowledged = blue check, Resolved = green check)
- [ ] Empty state shown when no alerts match filters

**Pagination:**
- [ ] Shows "Showing X-Y of Z alerts"
- [ ] Page size selector (10, 20, 50, 100)
- [ ] First/Previous/Next/Last buttons work
- [ ] Page numbers clickable
- [ ] Ellipsis shown for large page counts

**Loading States:**
- [ ] Skeleton rows shown while loading
- [ ] Smooth transition when data loads

---

### 4. Alert Detail Modal

**Open Modal:**
1. Click any alert row in the table
2. URL updates to `/alerts?alertId=xxx`

**Verify Modal Contents:**
- [ ] Header shows severity badge and status
- [ ] Alert type as title
- [ ] Full message displayed
- [ ] Alert Details section:
  - Alert ID
  - Created timestamp (relative time)
  - Acknowledged timestamp (if applicable)
  - Prediction ID (if applicable)
- [ ] Shipment Context section:
  - Clickable shipment link
  - Location coordinates (if available)
  - Speed reading (if available)
- [ ] Additional Details (raw JSON) if present
- [ ] Comment textarea visible for adding notes

**Actions (ADMIN/OPS_USER only):**
- [ ] Acknowledge button visible for OPEN alerts
- [ ] Resolve button visible for non-resolved alerts
- [ ] Both buttons hidden for VIEWER role

**Acknowledge Action:**
1. Open an OPEN alert
2. Optionally add comment
3. Click "Acknowledge"
4. Verify:
   - [ ] Modal stays open
   - [ ] Status changes to ACKNOWLEDGED
   - [ ] Acknowledge button disappears
   - [ ] Comment saved

**Resolve Action:**
1. Open a non-resolved alert
2. Optionally add comment
3. Click "Resolve"
4. Verify:
   - [ ] Modal closes
   - [ ] Alert marked as RESOLVED in table
   - [ ] Comment saved

**Close Modal:**
- [ ] Escape key closes modal
- [ ] Backdrop click closes modal
- [ ] Close button works
- [ ] URL resets to `/alerts`

**Loading/Error States:**
- [ ] Spinner shown while fetching alert
- [ ] Error message with retry button on failure

---

### 5. Bulk Actions

**Select Multiple Alerts:**
1. Check 2+ alerts in the table
2. Bulk action bar appears above table

**Bulk Acknowledge:**
1. Select multiple OPEN alerts
2. Click "Acknowledge" in bulk action bar
3. Verify:
   - [ ] All selected alerts marked as ACKNOWLEDGED
   - [ ] Selection cleared
   - [ ] Counts updated

**Bulk Resolve:**
1. Select multiple non-resolved alerts
2. Click "Resolve"
3. Optionally enter comment
4. Click "Confirm"
5. Verify:
   - [ ] All selected alerts marked as RESOLVED
   - [ ] Selection cleared
   - [ ] Counts updated

**Clear Selection:**
- [ ] X button clears selection
- [ ] Changing page/filters clears selection

---

### 6. Refresh & Export

**Refresh:**
1. Click "Refresh" button in page header
2. Verify:
   - [ ] Button shows spinning icon
   - [ ] Data reloads
   - [ ] Counts update

**Export (Placeholder):**
1. Click "Export" button
2. Currently logs to console (TODO)

---

### 7. RBAC Testing

**ADMIN Role:**
- [ ] Can view alerts page
- [ ] Can see notification bell
- [ ] Can acknowledge alerts
- [ ] Can resolve alerts
- [ ] Can perform bulk actions

**OPS_USER Role:**
- [ ] Can view alerts page
- [ ] Can see notification bell
- [ ] Can acknowledge alerts
- [ ] Can resolve alerts
- [ ] Can perform bulk actions

**VIEWER Role:**
- [ ] Cannot access `/alerts` (redirected to Unauthorized)
- [ ] Cannot see notification bell
- [ ] No action buttons in modals

---

### 8. Responsive Design

Test on various screen sizes:

**Desktop (1920x1080):**
- [ ] All features visible
- [ ] Table columns properly spaced
- [ ] Modal centered

**Laptop (1366x768):**
- [ ] Layout adjusts properly
- [ ] No horizontal scroll
- [ ] Modal fits in viewport

**Tablet (768x1024):**
- [ ] Filters wrap appropriately
- [ ] Table scrolls horizontally if needed
- [ ] Summary cards stack to 2 columns

**Mobile (375x667):**
- [ ] Summary cards stack to 1 column
- [ ] Filters stack vertically
- [ ] Modal takes full width with padding

---

### 9. Error Scenarios

**Network Errors:**
1. Disconnect network
2. Verify:
   - [ ] Error message shown
   - [ ] Retry button appears
   - [ ] SSE falls back to polling (connection dot turns yellow/red)

**SSE Disconnection:**
1. Stop backend prediction service
2. Verify:
   - [ ] After ~5s, connection switches to polling
   - [ ] Connection dot turns yellow/red
   - [ ] Alerts still load via REST

**Error Boundary:**
1. Trigger React error (e.g., modify component to throw)
2. Verify:
   - [ ] Error boundary catches error
   - [ ] User-friendly error message shown
   - [ ] "Reload Page" button works

---

### 10. Performance

**Initial Load:**
- [ ] Page loads within 2 seconds
- [ ] Skeleton loaders prevent layout shift
- [ ] No console errors

**Real-Time Updates:**
- [ ] New alerts appear instantly (via SSE)
- [ ] Unread count updates immediately
- [ ] Toast notifications appear smoothly

**Table Performance:**
- [ ] Sorting is instant
- [ ] Filtering is smooth (no lag)
- [ ] Pagination is fast

---

## Known Limitations

1. **CSV Export:** Not yet implemented (placeholder)
2. **Browser Notifications:** Permission handling not fully implemented
3. **Audit Events:** Not yet wired to backend
4. **Test Coverage:** Unit/integration tests pending

---

## Troubleshooting

### Bell Icon Not Showing
- Check user role (must be ADMIN or OPS_USER)
- Verify logged in
- Check browser console for errors

### SSE Not Connecting
- Verify backend prediction service is running
- Check CORS configuration
- Look for connection errors in browser console
- System will auto-fallback to polling

### Filters Not Working
- Check browser console for errors
- Verify backend API response format
- Try resetting filters

### Modal Not Opening
- Check URL for `alertId` parameter
- Verify alert exists
- Check browser console for errors

---

## Debug Checklist

If something isn't working:

1. **Check browser console** for errors
2. **Check network tab** for failed API calls
3. **Verify backend services** are running
4. **Check user role** matches requirements
5. **Try hard refresh** (Ctrl+Shift+R)
6. **Clear localStorage** if auth issues

---

## Support

For issues or questions:
- Check backend logs: `logs/prediction-engine-service.log`
- Check frontend console in browser DevTools
- Review API responses in Network tab
